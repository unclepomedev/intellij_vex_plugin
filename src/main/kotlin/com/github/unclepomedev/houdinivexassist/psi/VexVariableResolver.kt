package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement

object VexVariableResolver {
    /**
     * Resolves the declaration of a variable by tracing the ancestor scopes.
     * Returns the PsiElement where the variable is declared, or null if unresolved.
     */
    fun resolveVariable(element: PsiElement, varName: String): PsiElement? {
        val visibleVariables = VexScopeAnalyzer.getVisibleVariables(element)
        return visibleVariables.find {
            val ident = if (it is VexDeclarationItem) it.identifier else (it as VexParameterDef).identifier
            ident.text == varName
        }
    }
}
