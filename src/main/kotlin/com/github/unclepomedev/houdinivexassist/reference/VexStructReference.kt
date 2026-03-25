package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexElementFactory
import com.github.unclepomedev.houdinivexassist.psi.VexScopeAnalyzer
import com.github.unclepomedev.houdinivexassist.psi.VexTypeRef
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VexStructReference(
    element: VexTypeRef,
    textRange: TextRange
) : PsiReferenceBase<VexTypeRef>(element, textRange) {

    override fun resolve(): PsiElement? {
        val typeName = element.identifier?.text ?: return null
        val structs = VexScopeAnalyzer.getVisibleStructs(element)
        return structs.find { it.identifier?.text == typeName }
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val id = element.identifier ?: return element
        val newId = VexElementFactory.createIdentifier(element.project, newElementName)
        id.replace(newId)
        return element
    }
}
