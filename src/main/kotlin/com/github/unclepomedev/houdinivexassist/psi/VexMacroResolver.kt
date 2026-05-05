package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object VexMacroResolver {
    private fun resolveInFile(
        file: PsiFile,
        name: String,
        maxOffsetExclusive: Int,
        visited: MutableSet<String>,
        skipActiveCheck: Boolean = false
    ): VexMacroDef? {
        val key = VexFile.getFileKey(file)

        if (!visited.add(key)) {
            return null
        }

        try {
            var best: VexMacroDef? = null

            val events = mutableListOf<PsiElement>().apply {
                addAll(PsiTreeUtil.findChildrenOfType(file, VexMacroDef::class.java))
                addAll(PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java))
            }.filter { it.textOffset < maxOffsetExclusive }
                .sortedBy { it.textOffset }

            for (event in events) {
                if (!skipActiveCheck && !VexPreprocessorEvaluator.isActive(event)) continue
                when (event) {
                    is VexMacroDef -> if (event.identifier?.text == name) best = event
                    is VexIncludeDirective -> {
                        val includedPsi = VexScopeAnalyzer.resolveIncludeFile(event, file) ?: continue
                        val vexFile = (includedPsi as? VexFile)
                            ?: VexScopeAnalyzer.getIncludedFiles(includedPsi).firstOrNull()
                            ?: continue
                        val nested = resolveInFile(vexFile, name, Int.MAX_VALUE, visited, skipActiveCheck)
                        if (nested != null) best = nested
                    }
                }
            }
            return best
        } finally {
            visited.remove(key)
        }
    }

    fun resolveMacro(context: PsiElement, name: String): PsiElement? {
        val file = context.containingFile ?: return null
        return resolveInFile(file, name, context.textOffset, mutableSetOf())
    }

    fun resolveMacroForPreprocessor(context: PsiElement, name: String): PsiElement? {
        val file = context.containingFile ?: return null
        return resolveInFile(file, name, context.textOffset, mutableSetOf(), skipActiveCheck = true)
    }
}
