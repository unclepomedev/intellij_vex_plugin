package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil

object VexPreprocessorEvaluator {

    private data class BranchState(var hasTakenBranch: Boolean, var isActive: Boolean)

    private val evaluatingFiles = ThreadLocal.withInitial { mutableSetOf<String>() }

    fun isActive(element: PsiElement?): Boolean {
        if (element == null || element is PsiFile) return true
        val file = element.containingFile ?: return true

        val key = file.getUserData(VexFile.ORIGINAL_FILE_PATH_KEY)
            ?: file.originalFile.virtualFile?.path
            ?: file.name

        val visited = evaluatingFiles.get()
        if (visited.contains(key)) {
            // If already evaluating this file, assume active to avoid infinite recursion
            return true
        }
        visited.add(key)

        try {
            val inactiveRanges = getInactiveRanges(file)
            val offset = element.textOffset
            val isActive = inactiveRanges.none { offset in it.startOffset until it.endOffset }
            return isActive
        } finally {
            visited.remove(key)
        }
    }

    private fun getInactiveRanges(file: PsiFile): List<TextRange> {
        return CachedValuesManager.getCachedValue(file) {
            val ranges = computeInactiveRanges(file)
            com.intellij.psi.util.CachedValueProvider.Result.create(ranges, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    private fun computeInactiveRanges(file: PsiFile): List<TextRange> {
        val directives = PsiTreeUtil.findChildrenOfType(file, VexPreprocessorDirective::class.java)
        val result = mutableListOf<TextRange>()
        val stack = mutableListOf<BranchState>()
        var inactiveStart: Int? = null

        for (directive in directives) {
            val wasActive = isStackActive(stack)
            updateStack(stack, directive)
            inactiveStart = trackInactiveRange(wasActive, isStackActive(stack), directive, inactiveStart, result)
        }

        if (inactiveStart != null) {
            result.add(TextRange(inactiveStart, file.textLength))
        }

        return result
    }

    private fun isStackActive(stack: List<BranchState>): Boolean =
        stack.isEmpty() || stack.all { it.isActive }

    private fun updateStack(stack: MutableList<BranchState>, directive: VexPreprocessorDirective) {
        when {
            directive.ppIfdef != null -> pushIfdef(stack, directive)
            directive.ppIfndef != null -> pushIfndef(stack, directive)
            directive.ppIf != null -> pushIf(stack, directive)
            directive.ppElif != null -> handleElif(stack, directive)
            directive.ppElse != null -> handleElse(stack)
            directive.ppEndif != null -> handleEndif(stack)
            directive.ppUndef != null -> {}
        }
    }

    private fun pushIfdef(stack: MutableList<BranchState>, directive: VexPreprocessorDirective) {
        val macroName = directive.ppIfdef!!.identifier?.text
        val defined = macroName != null &&
                VexMacroResolver.resolveMacro(directive, macroName) != null
        stack.add(BranchState(hasTakenBranch = defined, isActive = defined))
    }

    private fun pushIfndef(stack: MutableList<BranchState>, directive: VexPreprocessorDirective) {
        val macroName = directive.ppIfndef!!.identifier?.text
        val defined = macroName != null &&
                VexMacroResolver.resolveMacro(directive, macroName) != null
        stack.add(BranchState(hasTakenBranch = !defined, isActive = !defined))
    }

    private fun pushIf(stack: MutableList<BranchState>, directive: VexPreprocessorDirective) {
        val conditionText = directive.ppIf?.macroBody?.text
        val result = VexConditionEvaluator.evaluate(conditionText, directive)
        stack.add(BranchState(hasTakenBranch = result, isActive = result))
    }

    private fun handleElif(stack: MutableList<BranchState>, directive: VexPreprocessorDirective) {
        if (stack.isNotEmpty()) {
            val state = stack.last()
            if (state.hasTakenBranch) {
                state.isActive = false
            } else {
                val conditionText = directive.ppElif?.macroBody?.text
                val result = VexConditionEvaluator.evaluate(conditionText, directive)
                state.isActive = result
                if (result) state.hasTakenBranch = true
            }
        }
    }

    private fun handleElse(stack: MutableList<BranchState>) {
        if (stack.isNotEmpty()) {
            val state = stack.last()
            state.isActive = !state.hasTakenBranch
        }
    }

    private fun handleEndif(stack: MutableList<BranchState>) {
        if (stack.isNotEmpty()) {
            stack.removeAt(stack.lastIndex)
        }
    }

    private fun trackInactiveRange(
        wasActive: Boolean,
        nowActive: Boolean,
        directive: VexPreprocessorDirective,
        inactiveStart: Int?,
        result: MutableList<TextRange>
    ): Int? {
        if (wasActive && !nowActive) {
            return directive.textRange.endOffset
        }
        if (!wasActive && nowActive && inactiveStart != null) {
            result.add(TextRange(inactiveStart, directive.textOffset))
            return null
        }
        return inactiveStart
    }
}
