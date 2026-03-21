package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexElementFactory
import com.github.unclepomedev.houdinivexassist.psi.VexPrimaryExpr
import com.github.unclepomedev.houdinivexassist.reference.VexVariableReference
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

abstract class VexPrimaryExprMixin(node: ASTNode) : VexExprImpl(node), VexPrimaryExpr {

    override fun getName(): String? = identifier?.text

    override fun setName(name: String): PsiElement {
        val id = identifier
        if (id != null) {
            val newId = VexElementFactory.createIdentifier(project, name)
            id.replace(newId)
        }
        return this
    }

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun getReference(): PsiReference? {
        val id = identifier ?: return null
        return VexVariableReference(this, id.textRangeInParent)
    }
}
