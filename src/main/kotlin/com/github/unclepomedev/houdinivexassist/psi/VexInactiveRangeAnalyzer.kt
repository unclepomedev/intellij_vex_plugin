package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object VexInactiveRangeAnalyzer {

    fun analyze(file: PsiFile): List<TextRange> = analyze(file, emptySet())

    /**
     * [seedDefinedMacros] lets callers (e.g., VexMacroResolver recursing into includes)
     * inject macros already defined by the parent file so #ifdef inside the included
     * file evaluates with the correct context instead of an empty slate.
     */
    fun analyze(file: PsiFile, seedDefinedMacros: Set<String>): List<TextRange> {
        val definedMacros = seedDefinedMacros.toMutableSet()
        val visitedIncludes = mutableSetOf(VexFile.getFileKey(file))
        return collect(file, definedMacros, visitedIncludes, trackRanges = true)
    }

    private fun collect(
        file: PsiFile,
        definedMacros: MutableSet<String>,
        visited: MutableSet<String>,
        trackRanges: Boolean
    ): List<TextRange> {
        val stack = VexBranchStack(file.textLength, trackRanges = trackRanges)
        val events = collectEvents(file)

        for (event in events) {
            val wasActive = stack.isActive

            when (event) {
                is VexPreprocessorDirective -> stack.processDirective(event, definedMacros)
                is VexMacroDef -> {
                    if (wasActive) event.identifier?.text?.let { definedMacros.add(it) }
                }

                is VexIncludeDirective -> if (wasActive) {
                    val resolved = VexIncludeResolver.resolveIncludeFile(event, file) ?: continue
                    val vexFile = VexSyntheticFileProvider.getAsVexFile(resolved) ?: continue

                    val key = VexFile.getFileKey(vexFile)
                    if (visited.add(key)) {
                        try {
                            // Sub-files only contribute to definedMacros, not to the parent's inactive ranges.
                            collect(vexFile, definedMacros, visited, trackRanges = false)
                        } finally {
                            visited.remove(key)
                        }
                    }
                }
            }
        }
        return stack.finalizeRanges()
    }

    private fun collectEvents(file: PsiFile): List<PsiElement> {
        val list = mutableListOf<PsiElement>()
        file.accept(object : VexVisitor() {
            override fun visitPreprocessorDirective(o: VexPreprocessorDirective) {
                list.add(o)
            }

            override fun visitMacroDef(o: VexMacroDef) {
                list.add(o)
            }

            override fun visitIncludeDirective(o: VexIncludeDirective) {
                list.add(o)
            }

            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }
        })

        return list.sortedWith(compareBy({ it.textOffset }, {
            when (it) {
                is VexPreprocessorDirective -> 0
                is VexMacroDef -> 1
                else -> 2
            }
        }))
    }
}
