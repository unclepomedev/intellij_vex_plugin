package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexPrimaryExpr
import com.github.unclepomedev.houdinivexassist.reference.VexVariableReference
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference

abstract class VexPrimaryExprMixin(node: ASTNode) : VexExprImpl(node), VexPrimaryExpr {

    override fun getReference(): PsiReference? {
        val id = identifier ?: return null
        return VexVariableReference(this, id.textRangeInParent, id.text)
    }
}
