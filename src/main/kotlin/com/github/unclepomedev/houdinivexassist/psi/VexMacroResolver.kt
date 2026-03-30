package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object VexMacroResolver {
    private fun resolveInFile(
        file: PsiFile,
        name: String,
        maxOffsetExclusive: Int,
        visited: MutableSet<String>
    ): VexMacroDef? {
        val key = file.originalFile.virtualFile?.path ?: file.name
        if (!visited.add(key)) return null

        var best: VexMacroDef? = null

        val events = mutableListOf<PsiElement>().apply {
            addAll(PsiTreeUtil.findChildrenOfType(file, VexMacroDef::class.java))
            addAll(PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java))
        }.filter { it.textOffset < maxOffsetExclusive }
            .sortedBy { it.textOffset }

        for (event in events) {
            when (event) {
                is VexMacroDef -> if (event.identifier?.text == name) best = event
                is VexIncludeDirective -> {
                    val includedPsi = VexScopeAnalyzer.resolveIncludeFile(event) ?: continue
                    // re-parsed as VexFile via getIncludedFiles; take first to get converted file only
                    val vexFile = VexScopeAnalyzer.getIncludedFiles(includedPsi).firstOrNull() ?: continue
                    val nested = resolveInFile(vexFile, name, Int.MAX_VALUE, visited)
                    if (nested != null) best = nested
                }
            }
        }
        return best
    }

    fun resolveMacro(context: PsiElement, name: String): PsiElement? {
        val file = context.containingFile ?: return null
        return resolveInFile(file, name, context.textOffset, mutableSetOf())
    }
}
