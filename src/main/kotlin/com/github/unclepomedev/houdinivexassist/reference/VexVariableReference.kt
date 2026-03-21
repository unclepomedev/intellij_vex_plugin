package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexNamedElement
import com.github.unclepomedev.houdinivexassist.psi.VexVariableResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache

class VexVariableReference(
    element: PsiElement,
    textRange: TextRange,
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        return ResolveCache.getInstance(element.project).resolveWithCaching(
            this,
            Resolver,
            false,
            false
        )
    }

    private object Resolver : ResolveCache.AbstractResolver<VexVariableReference, PsiElement> {
        override fun resolve(ref: VexVariableReference, incompleteCode: Boolean): PsiElement? {
            val identifierName = ref.rangeInElement.substring(ref.element.text)
            return VexVariableResolver.resolveVariable(ref.element, identifierName)
        }
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return (element as VexNamedElement).setName(newElementName)
    }
}
