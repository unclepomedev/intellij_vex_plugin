package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexTypeRef
import com.github.unclepomedev.houdinivexassist.reference.VexStructReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference

abstract class VexTypeRefMixin(node: ASTNode) : ASTWrapperPsiElement(node), VexTypeRef {
    override fun getReference(): PsiReference? {
        val id = identifier ?: return null
        return VexStructReference(this, id.textRangeInParent)
    }
}
