package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexVariableResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VexVariableReference(
    element: PsiElement,
    textRange: TextRange,
    private val name: String
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        return VexVariableResolver.resolveVariable(element, name)
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }
}
