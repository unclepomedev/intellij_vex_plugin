package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.settings.VexSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import java.io.File

object VexScopeAnalyzer {
    private data class SyntheticCacheEntry(
        val modificationStamp: Long,
        val filePath: String,
        val fileName: String,
        val vexFile: VexFile,
    )

    private val SYNTHETIC_VEX_FILE_KEY = Key.create<SyntheticCacheEntry>("VEX_SYNTHETIC_FILE")

    fun getOrCreateSyntheticVexFile(current: PsiFile): VexFile {
        val stamp = current.modificationStamp
        val originalPath = current.originalFile.virtualFile?.path ?: current.name
        val name = current.name
        val cached = current.getUserData(SYNTHETIC_VEX_FILE_KEY)
        if (
            cached != null &&
                cached.modificationStamp == stamp &&
                cached.filePath == originalPath &&
                cached.fileName == name
        ) {
            return cached.vexFile
        }
        val parsed =
            PsiFileFactory.getInstance(current.project)
                .createFileFromText(
                    name,
                    VexLanguage.INSTANCE,
                    current.text,
                ) as VexFile
        parsed.putUserData(
            VexMacroResolver.ORIGINAL_FILE_PATH_KEY,
            originalPath,
        )
        current.putUserData(
            SYNTHETIC_VEX_FILE_KEY,
            SyntheticCacheEntry(stamp, originalPath, name, parsed),
        )
        return parsed
    }

    private val includePathTracker = ModificationTracker {
        val settings = ApplicationManager.getApplication()?.getService(VexSettingsState::class.java)
        val includeHash = settings?.includePath?.hashCode()?.toLong() ?: 0L
        val hfsHash = settings?.hfsPath?.hashCode()?.toLong() ?: 0L
        includeHash xor (hfsHash shl 32)
    }

    /** Resolves the #include file path to a VirtualFile. */
    fun resolveIncludeFile(
        includeDirective: VexIncludeDirective,
        contextFile: PsiFile? = null,
    ): PsiFile? {
        val pathStringNode =
            includeDirective.string
                ?: includeDirective.unclosedString
                ?: includeDirective.sysString
                ?: includeDirective.unclosedSysString
                ?: return null
        val rawText = pathStringNode.text

        val fileName =
            rawText
                .removePrefix("\"")
                .removeSuffix("\"")
                .removePrefix("'")
                .removeSuffix("'")
                .removePrefix("<")
                .removeSuffix(">")

        if (fileName.isEmpty()) return null

        val currentFile = contextFile ?: includeDirective.containingFile ?: return null

        return resolveFromCurrentDirectory(currentFile, fileName)
            ?: resolveFromIncludePaths(currentFile.project, fileName)
    }

    private fun resolveFromCurrentDirectory(currentFile: PsiFile, fileName: String): PsiFile? {
        val currentDir = currentFile.originalFile.virtualFile?.parent ?: return null
        val file =
            currentDir.findFileByRelativePath(fileName)?.takeIf { !it.isDirectory } ?: return null
        return PsiManager.getInstance(currentFile.project).findFile(file)
    }

    private fun resolveDefaultIncludePath(hfsPath: String): String {
        if (hfsPath.isEmpty()) return ""
        val macPath =
            "$hfsPath/Frameworks/Houdini.framework/Versions/Current/Resources/houdini/vex/include"
        if (File(macPath).exists()) return macPath
        val fallback = "$hfsPath/houdini/vex/include"
        if (File(fallback).exists()) return fallback
        return ""
    }

    fun parseIncludePaths(
        includePathStr: String,
        pathSeparator: String = File.pathSeparator,
    ): List<String> {
        val settingsState =
            ApplicationManager.getApplication()?.getService(VexSettingsState::class.java)
        val hfsPath = settingsState?.hfsPath ?: ""
        val defaultInclude by lazy(LazyThreadSafetyMode.NONE) { resolveDefaultIncludePath(hfsPath) }

        // (?<!^[a-zA-Z]) : Backtracking. If the first character is a single letter (e.g., C:), do
        // not split it.
        // (?!//|\\\\)    : Do not split URL schemes (://) or Windows backslashes (:\).
        val colonSplitter = Regex("(?<!^[a-zA-Z]):(?!//|\\\\)")

        return includePathStr
            .split(";")
            .flatMap { rawSegment ->
                val segment =
                    rawSegment
                        .trim() // To ensure the ^ (leading character) in regular expressions works
                // correctly, trim first.
                if (pathSeparator == ":") segment.split(colonSplitter) else listOf(segment)
            }
            .map { segment ->
                val trimmed = segment.trim()
                if (trimmed == "&") defaultInclude else trimmed
            }
            .filter { it.isNotEmpty() }
    }

    private fun resolveFromIncludePaths(project: Project, fileName: String): PsiFile? {
        val settingsState =
            ApplicationManager.getApplication()?.getService(VexSettingsState::class.java)
        val includePathStr = settingsState?.includePath ?: return null

        return parseIncludePaths(includePathStr)
            .asSequence()
            .mapNotNull { findDirectoryByPathOrUrl(it) }
            .mapNotNull { dir -> dir.findFileByRelativePath(fileName) }
            .filter { !it.isDirectory }
            .firstNotNullOfOrNull { PsiManager.getInstance(project).findFile(it) }
    }

    private fun findDirectoryByPathOrUrl(path: String) =
        LocalFileSystem.getInstance().findFileByPath(path)?.takeIf { it.isDirectory }
            ?: com.intellij.openapi.vfs.VirtualFileManager.getInstance()
                .findFileByUrl(path)
                ?.takeIf { it.isDirectory }

    /**
     * Recursively retrieves the specified VexFile and all files it includes. Prevents infinite
     * loops caused by circular references.
     */
    fun getIncludedFiles(file: PsiFile): List<VexFile> {
        return CachedValuesManager.getCachedValue(file) {
            val result = mutableListOf<VexFile>()
            val visited = mutableSetOf<String>()

            fun visit(current: PsiFile) {
                val path = current.originalFile.virtualFile?.path ?: current.name
                if (!visited.add(path)) return

                val vexFile = current as? VexFile ?: getOrCreateSyntheticVexFile(current)
                result.add(vexFile)

                PsiTreeUtil.findChildrenOfType(vexFile, VexIncludeDirective::class.java)
                    .asSequence()
                    .filter { VexPreprocessorEvaluator.isActive(it) }
                    .mapNotNull { resolveIncludeFile(it, current) }
                    .forEach(::visit)
            }

            visit(file)

            CachedValueProvider.Result.create(
                result.toList(),
                PsiModificationTracker.MODIFICATION_COUNT,
                includePathTracker,
            )
        }
    }

    /**
     * Finds the closest declaration scope (Block, Struct, or File) for the given element. Safely
     * returns null if the input element is null, or if no such scope exists.
     *
     * @param element The starting element to search upwards from.
     * @return The containing scope element, or null.
     */
    fun findDeclarationScope(element: PsiElement?): PsiElement? {
        if (element == null) return null
        return PsiTreeUtil.getParentOfType(
            element,
            VexBlock::class.java,
            VexForStatement::class.java,
            VexStructDef::class.java,
            VexFile::class.java,
        )
    }

    fun getDeclarationsInScope(scope: PsiElement): List<VexDeclarationItem> {
        return CachedValuesManager.getCachedValue(scope) {
            val decls =
                PsiTreeUtil.findChildrenOfType(scope, VexDeclarationItem::class.java).filter {
                    findDeclarationScope(it) == scope && VexPreprocessorEvaluator.isActive(it)
                }
            CachedValueProvider.Result.create(
                decls,
                scope,
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }
    }

    fun getParametersForScope(scope: PsiElement): List<VexParameterDef> {
        if (scope !is VexBlock || scope.parent !is VexFunctionDef) return emptyList()
        val funcDef = scope.parent as VexFunctionDef
        val paramList = funcDef.parameterListDef ?: return emptyList()

        return CachedValuesManager.getCachedValue(paramList) {
            val params =
                PsiTreeUtil.findChildrenOfType(paramList, VexParameterDef::class.java).filter {
                    VexPreprocessorEvaluator.isActive(it)
                }
            CachedValueProvider.Result.create(params, paramList)
        }
    }

    fun getVisibleFunctions(element: PsiElement): List<VexFunctionDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            val funcs = findInIncludedFiles(file, VexFunctionDef::class.java)
            CachedValueProvider.Result.create(
                funcs,
                PsiModificationTracker.MODIFICATION_COUNT,
                includePathTracker,
            )
        }
    }

    fun getVisibleStructs(element: PsiElement): List<VexStructDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            val structs = findInIncludedFiles(file, VexStructDef::class.java)
            CachedValueProvider.Result.create(
                structs,
                PsiModificationTracker.MODIFICATION_COUNT,
                includePathTracker,
            )
        }
    }

    fun getVisibleVariables(element: PsiElement): List<PsiElement> {
        val localVariables = getVisibleVariablesInHierarchy(element)
        val fileVariables = getVisibleVariablesInFileAndIncludes(element)
        return localVariables + fileVariables
    }

    private fun getVisibleVariablesInHierarchy(element: PsiElement): List<PsiElement> {
        return generateSequence(element.parent) { it.parent }
            .takeWhile { it !is VexFile }
            .flatMap { parent -> getVisibleVariablesInParent(parent, element) }
            .toList()
    }

    private fun getVisibleVariablesInParent(
        parent: PsiElement,
        targetElement: PsiElement,
    ): Sequence<PsiElement> = sequence {
        when (parent) {
            is VexBlock -> {
                yieldAll(getDeclarationsPriorTo(parent, targetElement.textOffset))
                yieldAll(getParametersForScope(parent))
            }
            is VexForStatement -> {
                yieldAll(getDeclarationsPriorTo(parent, targetElement.textOffset))
            }
            is VexForeachStatement -> {
                val body = parent.statement
                if (body != null && PsiTreeUtil.isAncestor(body, targetElement, false)) {
                    yieldAll(parent.foreachVarList)
                }
            }
        }
    }

    private fun getVisibleVariablesInFileAndIncludes(element: PsiElement): List<PsiElement> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        val fileDecls = getDeclarationsPriorTo(file, element.textOffset)
        val includeDecls =
            getIncludedFiles(file)
                .asSequence()
                .filter { it != file }
                .flatMap { getDeclarationsInScope(it) }

        return fileDecls + includeDecls
    }

    private fun getDeclarationsPriorTo(
        scope: PsiElement,
        textOffset: Int,
    ): List<VexDeclarationItem> =
        getDeclarationsInScope(scope).filter { it.textOffset < textOffset }

    fun getLocalFunctionNames(file: VexFile): Set<String> {
        return CachedValuesManager.getCachedValue(file) {
            val names =
                findInIncludedFiles(file, VexFunctionDef::class.java)
                    .mapNotNull { it.identifier.text }
                    .toSet()
            CachedValueProvider.Result.create(
                names,
                PsiModificationTracker.MODIFICATION_COUNT,
                includePathTracker,
            )
        }
    }

    private fun <T : PsiElement> findInIncludedFiles(
        file: VexFile,
        psiClass: Class<T>,
    ): List<T> =
        getIncludedFiles(file)
            .flatMap { f -> PsiTreeUtil.findChildrenOfType(f, psiClass) }
            .filter { VexPreprocessorEvaluator.isActive(it) }
}
