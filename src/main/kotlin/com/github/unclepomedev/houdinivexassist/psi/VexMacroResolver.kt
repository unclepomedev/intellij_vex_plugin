package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VexMacroResolver {
    fun resolveMacro(context: PsiElement, name: String): PsiElement? {
        val file = context.containingFile ?: return null

        // Search current file for macro_def before context
        val allMacroDefs = PsiTreeUtil.findChildrenOfType(file, VexMacroDef::class.java)
        val localMatch = allMacroDefs
            .filter { it.textOffset < context.textOffset }
            .lastOrNull { it.identifier?.text == name }
        if (localMatch != null) return localMatch

        // Search included files in include-directive order.
        // Local definitions take precedence over included definitions.
        // Among included files, the first definition found (in include order) wins.
        val includes = PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java)
            .filter { it.textOffset < context.textOffset }
        for (include in includes) {
            val includedPsi = VexScopeAnalyzer.resolveIncludeFile(include) ?: continue
            val includedFiles = VexScopeAnalyzer.getIncludedFiles(includedPsi)
            for (incFile in includedFiles) {
                val macros = PsiTreeUtil.findChildrenOfType(incFile, VexMacroDef::class.java)
                val match = macros.lastOrNull { it.identifier?.text == name }
                if (match != null) return match
            }
        }

        return null
    }
}
