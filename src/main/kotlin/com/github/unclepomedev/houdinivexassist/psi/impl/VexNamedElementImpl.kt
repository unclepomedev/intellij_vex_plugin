package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexElementFactory
import com.github.unclepomedev.houdinivexassist.psi.VexNamedElement
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class VexNamedElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), VexNamedElement {
    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        val oldIdentifier = nameIdentifier ?: return this
        val newIdentifier = VexElementFactory.createIdentifier(project, name)
        oldIdentifier.replace(newIdentifier)
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(VexTypes.IDENTIFIER)
    }
}
