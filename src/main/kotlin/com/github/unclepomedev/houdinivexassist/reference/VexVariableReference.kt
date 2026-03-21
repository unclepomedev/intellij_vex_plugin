package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexNamedElement
import com.github.unclepomedev.houdinivexassist.psi.VexVariableResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VexVariableReference(
    element: PsiElement,
    textRange: TextRange,
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        val identifierName = rangeInElement.substring(element.text)
        return VexVariableResolver.resolveVariable(element, identifierName)
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return (element as VexNamedElement).setName(newElementName)
    }
}
