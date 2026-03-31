package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexMacroResolver
import com.github.unclepomedev.houdinivexassist.psi.VexNamedElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VexMacroReference(
    element: PsiElement,
    textRange: TextRange,
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        val name = rangeInElement.substring(element.text)
        return VexMacroResolver.resolveMacro(element, name)
    }

    override fun getVariants(): Array<Any> = emptyArray()

    override fun handleElementRename(newElementName: String): PsiElement {
        val named = element as? VexNamedElement
            ?: return element
        return named.setName(newElementName)
    }
}
