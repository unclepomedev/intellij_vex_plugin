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

object VexPreprocessorEvaluator {

    /**
     * ThreadLocal guard to prevent infinite recursion during root file resolution.
     */
    private val resolvingRoot = ThreadLocal.withInitial { false }

    fun isActive(element: PsiElement?): Boolean {
        if (element == null || element is PsiFile) return true
        val file = element.containingFile ?: return true

        if (resolvingRoot.get()) {
            // Break the cycle: fall back to context-free analysis during root resolution.
            val ranges = VexInactiveRangeAnalyzer.analyze(file)
            val offset = element.textOffset
            return ranges.none { offset in it.startOffset until it.endOffset }
        }

        val inactiveRanges = getInactiveRanges(file)
        val offset = element.textOffset
        return inactiveRanges.none { offset in it.startOffset until it.endOffset }
    }

    private fun getInactiveRanges(file: PsiFile): List<TextRange> {
        return CachedValuesManager.getCachedValue(file) {
            val ranges = computeInactiveRanges(file)
            CachedValueProvider.Result.create(
                ranges,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker,
                ProjectRootModificationTracker.getInstance(file.project)
            )
        }
    }

    private fun computeInactiveRanges(file: PsiFile): List<TextRange> {
        val root = findRootIncludingFile(file)
        return if (root != null && root != file) {
            VexInactiveRangeAnalyzer.analyzeWithIncludes(root)[VexFile.getFileKey(file)]
                ?: VexInactiveRangeAnalyzer.analyze(file)
        } else {
            VexInactiveRangeAnalyzer.analyze(file)
        }
    }

    /**
     * Finds the top-level VEX file that transitively includes the given [file].
     * This allows header files to be evaluated using the macro context of their includer.
     * Returns the original [file] if no parent includer is found.
     */
    private fun findRootIncludingFile(file: PsiFile): PsiFile? {
        if (resolvingRoot.get()) return file as? VexFile
        resolvingRoot.set(true)
        try {
            val project = file.project
            val targetKey = VexFile.getFileKey(file)
            val virtualFiles = FileTypeIndex.getFiles(VexFileType, GlobalSearchScope.projectScope(project))
            var self: PsiFile? = null

            for (vf in virtualFiles) {
                val candidate = PsiManager.getInstance(project).findFile(vf) as? VexFile ?: continue
                if (VexFile.getFileKey(candidate) == targetKey) {
                    self = candidate
                    continue
                }
                if (transitivelyIncludes(candidate, targetKey, mutableSetOf())) {
                    return candidate
                }
            }
            return self ?: (file as? VexFile)
        } finally {
            resolvingRoot.set(false)
        }
    }

    /**
     * Recursively checks for inclusion without evaluating the active state of the `#include` directives.
     * This is required to prevent infinite loops when determining the root file.
     */
    private fun transitivelyIncludes(file: PsiFile, targetKey: String, visited: MutableSet<String>): Boolean {
        val key = VexFile.getFileKey(file)
        if (!visited.add(key)) return false
        val directives = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java)
        for (d in directives) {
            val resolved = VexIncludeResolver.resolveIncludeFile(d, file) ?: continue
            val vex = VexSyntheticFileProvider.getAsVexFile(resolved) ?: continue
            if (VexFile.getFileKey(vex) == targetKey) return true
            if (transitivelyIncludes(vex, targetKey, visited)) return true
        }
        return false
    }
}
