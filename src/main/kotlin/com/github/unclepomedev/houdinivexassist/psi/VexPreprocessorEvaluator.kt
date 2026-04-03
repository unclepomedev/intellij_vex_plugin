package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object VexPreprocessorEvaluator {

    fun isActive(element: PsiElement?): Boolean {
        if (element == null || element is PsiFile) return true
        if (!isActive(element.parent)) return false

        val stack = mutableListOf<Boolean>()
        var sibling = element.parent?.firstChild
        while (sibling != null && sibling !== element) {
            if (sibling is VexPreprocessorDirective) {
                evaluateDirective(sibling, stack)
            }
            sibling = sibling.nextSibling
        }

        return stack.isEmpty() || stack.all { it }
    }

    private fun evaluateDirective(directive: VexPreprocessorDirective, stack: MutableList<Boolean>) {
        when {
            directive.ppIfdef != null -> {
                val macroName = directive.ppIfdef!!.identifier?.text
                val defined = macroName != null &&
                        VexMacroResolver.resolveMacro(directive, macroName) != null
                stack.add(defined)
            }

            directive.ppIfndef != null -> {
                val macroName = directive.ppIfndef!!.identifier?.text
                val defined = macroName != null &&
                        VexMacroResolver.resolveMacro(directive, macroName) != null
                stack.add(!defined)
            }

            directive.ppIf != null -> {
                stack.add(true)
            }

            directive.ppElif != null -> {
                if (stack.isNotEmpty()) {
                    stack[stack.lastIndex] = true
                }
            }

            directive.ppElse != null -> {
                if (stack.isNotEmpty()) {
                    stack[stack.lastIndex] = !stack.last()
                }
            }

            directive.ppEndif != null -> {
                if (stack.isNotEmpty()) {
                    stack.removeAt(stack.lastIndex)
                }
            }

            directive.ppUndef != null -> {}
        }
    }
}
