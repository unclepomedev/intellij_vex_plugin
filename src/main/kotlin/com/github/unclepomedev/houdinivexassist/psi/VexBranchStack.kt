package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange

/**
 * Tracks the active/inactive state of preprocessor branches (#if, #ifdef, etc.).
 * When [trackRanges] is true, it also records the [TextRange]s of inactive code blocks.
 */
class VexBranchStack(
    private val maxTextLength: Int,
    private val trackRanges: Boolean = true,
) {
    private data class BranchState(var hasTakenBranch: Boolean, var isActive: Boolean)

    private val stack = mutableListOf<BranchState>()
    private val inactiveRanges = mutableListOf<TextRange>()
    private var inactiveStart: Int? = null

    val isActive: Boolean get() = stack.all { it.isActive }

    fun processDirective(directive: VexPreprocessorDirective, definedMacros: MutableSet<String>) {
        val wasActive = isActive

        when {
            directive.ppIfdef != null -> pushBranch(isDefined(directive.ppIfdef!!.identifier?.text, definedMacros))
            directive.ppIfndef != null -> pushBranch(!isDefined(directive.ppIfndef!!.identifier?.text, definedMacros))
            directive.ppIf != null -> pushBranch(
                VexConditionEvaluator.evaluate(
                    directive.ppIf?.macroBody?.text,
                    definedMacros
                )
            )

            directive.ppElif != null -> handleElif(directive.ppElif?.macroBody?.text, definedMacros)
            directive.ppElse != null -> handleElse()
            directive.ppEndif != null -> handleEndif()
            directive.ppUndef != null -> {
                if (isActive) directive.ppUndef?.identifier?.text?.let { definedMacros.remove(it) }
            }
        }

        if (trackRanges) trackRange(wasActive, directive)
    }

    fun finalizeRanges(): List<TextRange> {
        inactiveStart?.let { inactiveRanges.add(TextRange(it, maxTextLength)) }
        return inactiveRanges
    }

    private fun isDefined(name: String?, macros: Set<String>) = name != null && macros.contains(name)

    private fun pushBranch(condition: Boolean) {
        stack.add(BranchState(hasTakenBranch = condition, isActive = isActive && condition))
    }

    private fun handleElif(conditionText: String?, definedMacros: Set<String>) {
        if (stack.isEmpty()) return
        val state = stack.last()
        val parentActive = stack.dropLast(1).all { it.isActive }

        if (state.hasTakenBranch || !parentActive) {
            state.isActive = false
        } else {
            val result = VexConditionEvaluator.evaluate(conditionText, definedMacros)
            state.isActive = result
            if (result) state.hasTakenBranch = true
        }
    }

    private fun handleElse() {
        if (stack.isEmpty()) return
        val state = stack.last()
        val parentActive = stack.dropLast(1).all { it.isActive }
        state.isActive = parentActive && !state.hasTakenBranch
    }

    private fun handleEndif() {
        if (stack.isNotEmpty()) stack.removeLast()
    }

    private fun trackRange(wasActive: Boolean, directive: VexPreprocessorDirective) {
        val nowActive = isActive
        if (wasActive && !nowActive) {
            inactiveStart = directive.textRange.endOffset
        } else if (!wasActive && nowActive && inactiveStart != null) {
            inactiveRanges.add(TextRange(inactiveStart!!, directive.textOffset))
            inactiveStart = null
        }
    }
}
