package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.reference.VexFunctionReference
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference

abstract class VexCallExprMixin(node: ASTNode) : VexExprImpl(node), VexCallExpr {

    override fun getReference(): PsiReference? {
        val id = identifier
        val argCount = argumentList?.exprList?.size ?: 0
        return VexFunctionReference(this, id.textRangeInParent, id.text, argCount)
    }
}
