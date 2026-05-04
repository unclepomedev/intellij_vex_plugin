package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object VexMacroResolver {
    private val resolvingFiles = ThreadLocal.withInitial { mutableSetOf<String>() }

    private fun resolveInFile(
        file: PsiFile,
        sourceFile: PsiFile,
        name: String,
        maxOffsetExclusive: Int
    ): VexMacroDef? {
        val key = sourceFile.originalFile.virtualFile?.path ?: sourceFile.name
        val visited = resolvingFiles.get()
        if (!visited.add(key)) return null

        try {
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
                        val includedPsi = VexScopeAnalyzer.resolveIncludeFile(event, sourceFile) ?: continue
                        val vexFile = (includedPsi as? VexFile)
                            ?: VexScopeAnalyzer.getIncludedFiles(includedPsi).firstOrNull()
                            ?: continue
                        val nested = resolveInFile(vexFile, includedPsi, name, Int.MAX_VALUE)
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
        return resolveInFile(file, file, name, context.textOffset)
    }
}
