package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

object VexPreprocessorEvaluator {

    /**
     * ThreadLocal guard to prevent infinite recursion during root file resolution.
     */
    private val resolvingRoot = ThreadLocal.withInitial { false }

    fun isActive(element: PsiElement?): Boolean {
        if (element == null || element is PsiFile) return true
        val file = element.containingFile ?: return true
        val offset = element.textOffset

        val ranges = if (resolvingRoot.get()) {
            // Break the cycle: fall back to context-free analysis during root resolution.
            VexInactiveRangeAnalyzer.analyze(file)
        } else {
            getInactiveRanges(file)
        }

        return ranges.none { offset in it.startOffset until it.endOffset }
    }

    private fun getInactiveRanges(file: PsiFile): List<TextRange> {
        return CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(
                computeInactiveRanges(file),
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker,
                ProjectRootModificationTracker.getInstance(file.project)
            )
        }
    }

    private fun computeInactiveRanges(file: PsiFile): List<TextRange> {
        val root = findRootIncludingFile(file)
        if (root != null && root != file) {
            VexInactiveRangeAnalyzer.analyzeWithIncludes(root)[VexFile.getFileKey(file)]?.let { return it }
        }
        return VexInactiveRangeAnalyzer.analyze(file)
    }

    private fun findRootIncludingFile(file: PsiFile): PsiFile? {
        if (resolvingRoot.get()) return file as? VexFile

        return CachedValuesManager.getCachedValue(file) {
            resolvingRoot.set(true)
            val root = try {
                computeRootIncludingFile(file)
            } finally {
                resolvingRoot.set(false)
            }

            CachedValueProvider.Result.create(
                root,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker,
                ProjectRootModificationTracker.getInstance(file.project)
            )
        }
    }

    private fun computeRootIncludingFile(file: PsiFile): PsiFile? {
        val project = file.project
        val targetKey = VexFile.getFileKey(file)
        val virtualFiles = FileTypeIndex.getFiles(VexFileType, GlobalSearchScope.projectScope(project))

        val candidates = mutableListOf<VexFile>()
        var self: PsiFile? = null

        for (vf in virtualFiles) {
            val candidate = PsiManager.getInstance(project).findFile(vf) as? VexFile ?: continue
            if (VexFile.getFileKey(candidate) == targetKey) {
                self = candidate
                continue
            }
            if (transitivelyIncludes(candidate, targetKey, mutableSetOf())) {
                candidates.add(candidate)
            }
        }

        if (candidates.isEmpty()) return self ?: (file as? VexFile)

        // Prefer a root where the target file is reached through active branches.
        return candidates.sortedBy { it.virtualFile?.path }.firstOrNull { candidate ->
            VexInactiveRangeAnalyzer.analyzeWithIncludes(candidate).containsKey(targetKey)
        } ?: candidates.first()
    }

    /**
     * Recursively checks for inclusion without evaluating the active state of the `#include` directives.
     * This is required to prevent infinite loops when determining the root file.
     */
    private fun transitivelyIncludes(file: PsiFile, targetKey: String, visited: MutableSet<String>): Boolean {
        val key = VexFile.getFileKey(file)
        if (!visited.add(key)) return false
        val directives = PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java)
        for (d in directives) {
            val resolved = VexIncludeResolver.resolveIncludeFile(d, file) ?: continue
            val vex = VexSyntheticFileProvider.getAsVexFile(resolved) ?: continue

            if (VexFile.getFileKey(vex) == targetKey) return true
            if (transitivelyIncludes(vex, targetKey, visited)) return true
        }
        return false
    }
}
