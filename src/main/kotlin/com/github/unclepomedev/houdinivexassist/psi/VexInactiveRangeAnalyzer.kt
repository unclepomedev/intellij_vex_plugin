package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object VexInactiveRangeAnalyzer {

    fun analyze(file: PsiFile): List<TextRange> {
        val definedMacros = mutableSetOf<String>()
        val visitedIncludes = mutableSetOf(VexFile.getFileKey(file))
        val stack = VexBranchStack(file.textLength)

        collect(file, definedMacros, visitedIncludes, stack)
        return stack.finalizeRanges()
    }

    private fun collect(
        file: PsiFile,
        definedMacros: MutableSet<String>,
        visited: MutableSet<String>,
        stack: VexBranchStack
    ) {
        val events = collectEvents(file)

        for (event in events) {
            val wasActive = stack.isActive

            when (event) {
                is VexPreprocessorDirective -> stack.processDirective(event, definedMacros)
                is VexMacroDef -> if (wasActive) event.identifier?.text?.let { definedMacros.add(it) }
                is VexIncludeDirective -> if (wasActive) {
                    val includedPsi = VexScopeAnalyzer.resolveIncludeFile(event, file) as? VexFile ?: continue
                    val key = VexFile.getFileKey(includedPsi)
                    if (visited.add(key)) {
                        try {
                            collect(includedPsi, definedMacros, visited, stack)
                        } finally {
                            visited.remove(key)
                        }
                    }
                }
            }
        }
    }

    private fun collectEvents(file: PsiFile): List<PsiElement> {
        return (PsiTreeUtil.findChildrenOfType(file, VexPreprocessorDirective::class.java) +
                PsiTreeUtil.findChildrenOfType(file, VexMacroDef::class.java) +
                PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java))
            .sortedWith(compareBy({ it.textOffset }, {
                // Process directives first at the same offset to accurately affect subsequent macros
                when (it) {
                    is VexPreprocessorDirective -> 0
                    is VexMacroDef -> 1
                    else -> 2
                }
            }))
    }
}
