package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object VexMacroResolver {

    fun resolveMacro(context: PsiElement, name: String): PsiElement? {
        val file = context.containingFile ?: return null
        return resolveInFile(file, name, context.textOffset, mutableSetOf(), emptySet())
    }

    private fun resolveInFile(
        file: PsiFile,
        name: String,
        maxOffsetExclusive: Int,
        visited: MutableSet<String>,
        parentDefined: Set<String>,
    ): VexMacroDef? {
        val key = VexFile.getFileKey(file)
        if (!visited.add(key)) return null

        return try {
            val inactiveRanges = computeInactiveRanges(file, parentDefined)
            val definedSoFar = parentDefined.toMutableSet()

            var bestMacro: VexMacroDef? = null
            val events = collectEvents(file, maxOffsetExclusive)

            for (event in events) {
                if (!isActive(event, inactiveRanges)) continue

                when (event) {
                    is VexMacroDef -> {
                        event.identifier?.text?.let { definedSoFar.add(it) }
                        if (event.identifier?.text == name) bestMacro = event
                    }

                    is VexIncludeDirective -> {
                        val nestedMacro = resolveInInclude(event, file, name, visited, definedSoFar)
                        if (nestedMacro != null) bestMacro = nestedMacro
                    }
                }
            }
            bestMacro
        } finally {
            visited.remove(key)
        }
    }

    private fun computeInactiveRanges(file: PsiFile, parentDefined: Set<String>): List<TextRange>? {
        return if (parentDefined.isEmpty()) {
            null // top-level: defer to cached VexPreprocessorEvaluator.isActive
        } else {
            VexInactiveRangeAnalyzer.analyze(file, parentDefined)
        }
    }

    private fun collectEvents(file: PsiFile, maxOffsetExclusive: Int): List<PsiElement> {
        val macros = PsiTreeUtil.findChildrenOfType(file, VexMacroDef::class.java)
        val includes = PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java)

        return (macros + includes)
            .filter { it.textOffset < maxOffsetExclusive }
            .sortedBy { it.textOffset }
    }

    private fun isActive(event: PsiElement, inactiveRanges: List<TextRange>?): Boolean {
        if (inactiveRanges == null) {
            return VexPreprocessorEvaluator.isActive(event)
        }
        val offset = event.textOffset
        return inactiveRanges.none { offset in it.startOffset until it.endOffset }
    }

    private fun resolveInInclude(
        directive: VexIncludeDirective,
        currentFile: PsiFile,
        name: String,
        visited: MutableSet<String>,
        definedSoFar: Set<String>
    ): VexMacroDef? {
        val includedPsi = VexIncludeResolver.resolveIncludeFile(directive, currentFile) ?: return null

        // Parse non-VEX headers (e.g., .h) as synthetic VexFiles.
        val vexFile = includedPsi as? VexFile
            ?: VexSyntheticFileProvider.getAsVexFile(includedPsi)
            ?: return null

        return resolveInFile(vexFile, name, Int.MAX_VALUE, visited, definedSoFar.toSet())
    }
}
