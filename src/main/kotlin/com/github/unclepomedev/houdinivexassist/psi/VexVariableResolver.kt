package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement

object VexVariableResolver {
    /**
     * Resolves the declaration of a variable by tracing the ancestor scopes.
     * Returns the PsiElement where the variable is declared, or null if unresolved.
     */
    fun resolveVariable(element: PsiElement, varName: String): PsiElement? {
        var currentScope = VexScopeAnalyzer.findDeclarationScope(element)

        while (currentScope != null) {
            val decls = VexScopeAnalyzer.getDeclarationsInScope(currentScope)
            val decl = decls.find { it.identifier.text == varName && it.textOffset < element.textOffset }
            if (decl != null) return decl

            val params = VexScopeAnalyzer.getParametersForScope(currentScope)
            val param = params.find { it.identifier.text == varName }
            if (param != null) return param

            currentScope = VexScopeAnalyzer.findDeclarationScope(currentScope.parent)
        }

        return null
    }
}
