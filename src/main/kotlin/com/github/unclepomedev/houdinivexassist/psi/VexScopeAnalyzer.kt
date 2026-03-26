package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.settings.VexSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
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
    private val includePathTracker = ModificationTracker {
        ApplicationManager.getApplication().getService(VexSettingsState::class.java)?.includePath?.hashCode()?.toLong()
            ?: 0L
    }

    /**
     * Resolves the #include file path to a VirtualFile.
     */
    fun resolveIncludeFile(includeDirective: VexIncludeDirective, contextFile: PsiFile? = null): PsiFile? {
        val pathStringNode = includeDirective.string ?: includeDirective.unclosedString ?: includeDirective.sysString
        ?: includeDirective.unclosedSysString ?: return null
        val rawText = pathStringNode.text

        val fileName = rawText.removePrefix("\"").removeSuffix("\"")
            .removePrefix("'").removeSuffix("'")
            .removePrefix("<").removeSuffix(">")

        if (fileName.isEmpty()) return null

        val currentFile = contextFile ?: includeDirective.containingFile ?: return null

        return resolveFromCurrentDirectory(currentFile, fileName)
            ?: resolveFromIncludePaths(currentFile.project, fileName)
    }

    private fun resolveFromCurrentDirectory(currentFile: PsiFile, fileName: String): PsiFile? {
        val currentDir = currentFile.originalFile.virtualFile?.parent ?: return null
        val file = currentDir.findFileByRelativePath(fileName)

        if (file != null && !file.isDirectory) {
            return PsiManager.getInstance(currentFile.project).findFile(file)
        }
        return null
    }

    fun parseIncludePaths(includePathStr: String, pathSeparator: String = File.pathSeparator): List<String> {
        // (?<!^[a-zA-Z]) : Backtracking. If the first character is a single letter (e.g., C:), do not split it.
        // (?!//|\\\\)    : Do not split URL schemes (://) or Windows backslashes (:\).
        val colonSplitter = Regex("(?<!^[a-zA-Z]):(?!//|\\\\)")

        return includePathStr
            .split(";")
            .flatMap { rawSegment ->
                val segment = rawSegment.trim() // To ensure the ^ (leading character) in regular expressions works correctly, trim first.
                if (pathSeparator == ":") segment.split(colonSplitter) else listOf(segment)
            }
            .map { it.removeSuffix("&").trim() }
            .filter { it.isNotEmpty() }
    }

    private fun resolveFromIncludePaths(project: Project, fileName: String): PsiFile? {
        val settingsState = ApplicationManager.getApplication().getService(VexSettingsState::class.java)
        val includePathStr = settingsState?.includePath ?: return null

        val paths = parseIncludePaths(includePathStr)
        for (path in paths) {
            var dir = LocalFileSystem.getInstance().findFileByPath(path)
            if (dir == null) {
                dir = com.intellij.openapi.vfs.VirtualFileManager.getInstance().findFileByUrl(path)
            }

            if (dir != null && dir.isDirectory) {
                val file = dir.findFileByRelativePath(fileName)
                if (file != null && !file.isDirectory) {
                    return PsiManager.getInstance(project).findFile(file)
                }
            }
        }
        return null
    }

    /**
     * Recursively retrieves the specified VexFile and all files it includes.
     * Prevents infinite loops caused by circular references.
     */
    fun getIncludedFiles(file: PsiFile): List<VexFile> {
        return CachedValuesManager.getCachedValue(file) {
            val result = mutableListOf<VexFile>()
            val visited = mutableSetOf<String>()

            fun visit(current: PsiFile) {
                val path = current.originalFile.virtualFile?.path ?: current.name
                if (!visited.add(path)) return

                val vexFile = if (current is VexFile) {
                    current
                } else {
                    val project = current.project
                    CachedValuesManager.getCachedValue(current) {
                        val parsed = PsiFileFactory.getInstance(project)
                            .createFileFromText(current.name, VexLanguage.INSTANCE, current.text) as VexFile
                        CachedValueProvider.Result.create(parsed, current)
                    }
                }

                result.add(vexFile)

                val includes = PsiTreeUtil.findChildrenOfType(vexFile, VexIncludeDirective::class.java)
                for (include in includes) {
                    val resolved = resolveIncludeFile(include, current)
                    if (resolved != null) {
                        visit(resolved)
                    }
                }
            }

            visit(file)

            CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT, includePathTracker)
        }
    }

    /**
     * Finds the closest declaration scope (Block, Struct, or File) for the given element.
     * Safely returns null if the input element is null, or if no such scope exists.
     * @param element The starting element to search upwards from.
     * @return The containing scope element, or null.
     */
    fun findDeclarationScope(element: PsiElement?): PsiElement? {
        if (element == null) return null
        return PsiTreeUtil.getParentOfType(
            element,
            VexBlock::class.java,
            VexStructDef::class.java,
            VexFile::class.java
        )
    }

    fun getDeclarationsInScope(scope: PsiElement): List<VexDeclarationItem> {
        return CachedValuesManager.getCachedValue(scope) {
            val decls = PsiTreeUtil.findChildrenOfType(scope, VexDeclarationItem::class.java)
                .filter { findDeclarationScope(it) == scope }
            CachedValueProvider.Result.create(decls, scope)
        }
    }

    fun getParametersForScope(scope: PsiElement): List<VexParameterDef> {
        if (scope !is VexBlock || scope.parent !is VexFunctionDef) return emptyList()
        val funcDef = scope.parent as VexFunctionDef
        val paramList = funcDef.parameterListDef ?: return emptyList()

        return CachedValuesManager.getCachedValue(paramList) {
            val params = PsiTreeUtil.findChildrenOfType(paramList, VexParameterDef::class.java).toList()
            CachedValueProvider.Result.create(params, paramList)
        }
    }

    fun getVisibleFunctions(element: PsiElement): List<VexFunctionDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            val funcs = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexFunctionDef::class.java)
            }
            CachedValueProvider.Result.create(funcs, PsiModificationTracker.MODIFICATION_COUNT, includePathTracker)
        }
    }

    fun getVisibleStructs(element: PsiElement): List<VexStructDef> {
        val file = element.containingFile as? VexFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(file) {
            val structs = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexStructDef::class.java)
            }
            CachedValueProvider.Result.create(structs, PsiModificationTracker.MODIFICATION_COUNT, includePathTracker)
        }
    }

    fun getVisibleVariables(element: PsiElement): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        var currentScope = findDeclarationScope(element)
        while (currentScope != null) {
            if (currentScope is VexFile) {
                val decls = getDeclarationsInScope(currentScope)
                result.addAll(decls.filter { it.textOffset < element.textOffset })

                val includedFiles = getIncludedFiles(currentScope)
                for (incFile in includedFiles) {
                    if (incFile != currentScope) {
                        result.addAll(getDeclarationsInScope(incFile))
                    }
                }
            } else {
                val decls = getDeclarationsInScope(currentScope)
                result.addAll(decls.filter { it.textOffset < element.textOffset })

                val params = getParametersForScope(currentScope)
                result.addAll(params)
            }

            currentScope = findDeclarationScope(currentScope.parent)
        }
        return result
    }

    fun getLocalFunctionNames(file: VexFile): Set<String> {
        return CachedValuesManager.getCachedValue(file) {
            val names = getIncludedFiles(file).flatMap { f ->
                PsiTreeUtil.findChildrenOfType(f, VexFunctionDef::class.java)
            }.mapNotNull { it.identifier.text }.toSet()
            CachedValueProvider.Result.create(names, PsiModificationTracker.MODIFICATION_COUNT, includePathTracker)
        }
    }
}
