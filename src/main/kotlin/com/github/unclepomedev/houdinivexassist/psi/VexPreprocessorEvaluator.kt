package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

object VexPreprocessorEvaluator {
    fun isActive(element: PsiElement?): Boolean {
        if (element == null || element is PsiFile) return true
        val file = element.containingFile ?: return true

        val inactiveRanges = getInactiveRanges(file)
        val offset = element.textOffset
        return inactiveRanges.none { offset in it.startOffset until it.endOffset }
    }

    private fun getInactiveRanges(file: PsiFile): List<TextRange> {
        return CachedValuesManager.getCachedValue(file) {
            val ranges = VexInactiveRangeAnalyzer.analyze(file)
            CachedValueProvider.Result.create(
                ranges,
                PsiModificationTracker.MODIFICATION_COUNT,
                VexIncludeResolver.includePathTracker
            )
        }
    }
}
