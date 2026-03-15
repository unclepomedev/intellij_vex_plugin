package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil

object VexScopeAnalyzer {
    /**
     * Finds the closest declaration scope (Block, Struct, or File) for the given element.
     * Safely returns null if the input element is null, or if no such scope exists.
     * @param element The starting element to search upwards from.
     * @return The containing scope element, or null.
     */
    fun findDeclarationScope(element: PsiElement?): PsiElement? {
        if (element == null) return null
        return PsiTreeUtil.getParentOfType(
            element,
            VexBlock::class.java,
            VexStructDef::class.java,
            VexFile::class.java
        )
    }

    fun getDeclarationsInScope(scope: PsiElement): List<VexDeclarationItem> {
        return CachedValuesManager.getCachedValue(scope) {
            val decls = PsiTreeUtil.findChildrenOfType(scope, VexDeclarationItem::class.java)
                .filter { findDeclarationScope(it) == scope }
            CachedValueProvider.Result.create(decls, scope)
        }
    }

    fun getParametersForScope(scope: PsiElement): List<VexParameterDef> {
        if (scope !is VexBlock || scope.parent !is VexFunctionDef) return emptyList()
        val funcDef = scope.parent as VexFunctionDef
        val paramList = funcDef.parameterListDef ?: return emptyList()

        return CachedValuesManager.getCachedValue(paramList) {
            val params = PsiTreeUtil.findChildrenOfType(paramList, VexParameterDef::class.java).toList()
            CachedValueProvider.Result.create(params, paramList)
        }
    }

    fun getLocalFunctionNames(file: VexFile): Set<String> {
        return CachedValuesManager.getCachedValue(file) {
            val names = PsiTreeUtil.findChildrenOfType(file, VexFunctionDef::class.java)
                .mapNotNull { it.identifier.text }
                .toSet()
            CachedValueProvider.Result.create(names, file)
        }
    }
}
