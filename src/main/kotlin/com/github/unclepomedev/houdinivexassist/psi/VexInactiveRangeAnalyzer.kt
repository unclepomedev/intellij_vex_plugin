package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object VexInactiveRangeAnalyzer {

    fun analyze(file: PsiFile): List<TextRange> = analyze(file, emptySet())

    /**
     * Computes inactive ranges using a pre-existing set of defined macros.
     * This allows included files to inherit their parent's macro context,
     * ensuring `#ifdef` directives evaluate correctly.
     */
    fun analyze(file: PsiFile, seedDefinedMacros: Set<String>): List<TextRange> {
        val definedMacros = seedDefinedMacros.toMutableSet()
        return process(file, definedMacros, mutableSetOf(), outMap = null, trackRanges = true)
    }

    /**
     * Computes inactive ranges for the root file and all transitively included files.
     * By propagating the macro state down the include tree, this provides accurate
     * context-aware ranges for elements deep within headers.
     */
    fun analyzeWithIncludes(rootFile: PsiFile): Map<String, List<TextRange>> {
        val result = mutableMapOf<String, List<TextRange>>()
        process(rootFile, mutableSetOf(), mutableSetOf(), result, trackRanges = true)
        return result
    }

    private fun process(
        file: PsiFile,
        definedMacros: MutableSet<String>,
        inStack: MutableSet<String>,
        outMap: MutableMap<String, List<TextRange>>?,
        trackRanges: Boolean
    ): List<TextRange> {
        val key = VexFile.getFileKey(file)
        if (!inStack.add(key)) return emptyList()

        try {
            val shouldTrack = trackRanges && (outMap == null || key !in outMap)
            val stack = VexBranchStack(file.textLength, trackRanges = shouldTrack)

            for (event in collectEvents(file)) {
                val wasActive = stack.isActive
                when (event) {
                    is VexPreprocessorDirective -> stack.processDirective(event, definedMacros)
                    is VexMacroDef -> if (wasActive) event.identifier?.text?.let { definedMacros.add(it) }
                    is VexIncludeDirective -> if (wasActive) {
                        val resolved = VexIncludeResolver.resolveIncludeFile(event, file) ?: continue
                        val vexFile = VexSyntheticFileProvider.getAsVexFile(resolved) ?: continue
                        process(vexFile, definedMacros, inStack, outMap, trackRanges = outMap != null)
                    }
                }
            }

            val ranges = stack.finalizeRanges()
            if (shouldTrack && outMap != null) {
                outMap[key] = ranges
            }
            return ranges
        } finally {
            inStack.remove(key)
        }
    }

    private fun collectEvents(file: PsiFile): List<PsiElement> {
        val events = mutableListOf<PsiElement>()
        file.accept(object : VexVisitor() {
            override fun visitPreprocessorDirective(o: VexPreprocessorDirective) {
                events.add(o)
            }

            override fun visitMacroDef(o: VexMacroDef) {
                events.add(o)
            }

            override fun visitIncludeDirective(o: VexIncludeDirective) {
                events.add(o)
            }

            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }
        })

        return events.sortedWith(compareBy({ it.textOffset }, {
            when (it) {
                is VexPreprocessorDirective -> 0
                is VexMacroDef -> 1
                else -> 2
            }
        }))
    }
}
