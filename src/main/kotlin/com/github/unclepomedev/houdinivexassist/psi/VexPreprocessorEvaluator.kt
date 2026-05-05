package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

object VexPreprocessorEvaluator {

    // Break recursion during macro resolution to prevent definition leaks.
    private val evaluatingFiles = ThreadLocal.withInitial { mutableSetOf<String>() }

    fun isActive(element: PsiElement?): Boolean {
        if (element == null || element is PsiFile) return true
        val file = element.containingFile ?: return true
        val key = VexFile.getFileKey(file)

        val visited = evaluatingFiles.get()
        if (!visited.add(key)) return true // Prevent infinite recursion

        return try {
            val inactiveRanges = getInactiveRanges(file)
            val offset = element.textOffset
            inactiveRanges.none { offset in it.startOffset until it.endOffset }
        } finally {
            visited.remove(key)
        }
    }

    private fun getInactiveRanges(file: PsiFile): List<TextRange> {
        return CachedValuesManager.getCachedValue(file) {
            val ranges = VexInactiveRangeAnalyzer.analyze(file)
            CachedValueProvider.Result.create(ranges, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }
}
