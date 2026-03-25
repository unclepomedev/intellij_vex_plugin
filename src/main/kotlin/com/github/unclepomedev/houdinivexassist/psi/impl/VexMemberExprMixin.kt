package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexMemberExpr
import com.github.unclepomedev.houdinivexassist.reference.VexStructMemberReference
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference

abstract class VexMemberExprMixin(node: ASTNode) : VexExprImpl(node), VexMemberExpr {
    override fun getReference(): PsiReference? {
        val id = identifier ?: return null
        return VexStructMemberReference(this, id.textRangeInParent)
    }
}
