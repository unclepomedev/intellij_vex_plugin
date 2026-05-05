package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.settings.VexSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File

object VexIncludeResolver {

    val includePathTracker = ModificationTracker {
        val settings = ApplicationManager.getApplication()?.getService(VexSettingsState::class.java)
        val includeHash = settings?.includePath?.hashCode()?.toLong() ?: 0L
        val hfsHash = settings?.hfsPath?.hashCode()?.toLong() ?: 0L
        includeHash xor (hfsHash shl 32)
    }

    /**
     * Resolves the #include file path to a VirtualFile/PsiFile.
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
        // Fall back to URL-based resolution for synthetic or LightVirtualFiles where the parent directory is not directly accessible.
        val virtualFile = currentFile.originalFile.virtualFile
        val currentDir = virtualFile?.parent
            ?: currentFile.getUserData(VexFile.ORIGINAL_FILE_PATH_KEY)
                ?.let { VirtualFileManager.getInstance().findFileByUrl(it)?.parent }
            ?: virtualFile?.url
                ?.let { VirtualFileManager.getInstance().findFileByUrl(it)?.parent }
            ?: return null

        val file = currentDir.findFileByRelativePath(fileName)
        if (file != null && !file.isDirectory) {
            return PsiManager.getInstance(currentFile.project).findFile(file)
        }
        return null
    }

    private fun resolveDefaultIncludePath(hfsPath: String): String {
        if (hfsPath.isEmpty()) return ""
        val macPath = "$hfsPath/Frameworks/Houdini.framework/Versions/Current/Resources/houdini/vex/include"
        if (File(macPath).exists()) return macPath
        val fallback = "$hfsPath/houdini/vex/include"
        if (File(fallback).exists()) return fallback
        return ""
    }

    fun parseIncludePaths(includePathStr: String, pathSeparator: String = File.pathSeparator): List<String> {
        val settingsState = ApplicationManager.getApplication()?.getService(VexSettingsState::class.java)
        val hfsPath = settingsState?.hfsPath ?: ""
        val defaultInclude by lazy(LazyThreadSafetyMode.NONE) { resolveDefaultIncludePath(hfsPath) }

        val colonSplitter = Regex("(?<!^[a-zA-Z]):(?!//|\\\\)")

        return includePathStr
            .split(";")
            .flatMap { rawSegment ->
                val segment = rawSegment.trim()
                if (pathSeparator == ":") segment.split(colonSplitter) else listOf(segment)
            }
            .map { segment ->
                val trimmed = segment.trim()
                if (trimmed == "&") defaultInclude else trimmed
            }
            .filter { it.isNotEmpty() }
    }

    private fun resolveFromIncludePaths(project: Project, fileName: String): PsiFile? {
        val settingsState = ApplicationManager.getApplication()?.getService(VexSettingsState::class.java)
        val includePathStr = settingsState?.includePath ?: return null

        val paths = parseIncludePaths(includePathStr)
        for (path in paths) {
            var dir = LocalFileSystem.getInstance().findFileByPath(path)
            if (dir == null) {
                dir = VirtualFileManager.getInstance().findFileByUrl(path)
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
}