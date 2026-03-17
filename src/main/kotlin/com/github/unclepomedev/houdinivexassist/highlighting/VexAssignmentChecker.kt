package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.types.*
import com.intellij.psi.PsiElement

class VexAssignmentChecker(private val reporter: VexTypeCheckReporter) {
    fun check(element: VexAssignExpr) {
        val exprs = element.exprList
        if (exprs.size < 2) return

        val lhsExpr = exprs[0]
        val rhsExpr = exprs[1]
        val lhsType = VexTypeInference.inferType(lhsExpr)
        val rhsType = VexTypeInference.inferType(rhsExpr)
        val operatorKind = element.operatorKind

        if (operatorKind == null) {
            if (lhsType is VexType.ArrayType && isLiteralInitializerList(rhsExpr) && rhsType == VexType.UnknownType) return
            if (!VexTypePromotion.isAssignable(lhsType, rhsType)) {
                reporter.reportIncompatibleAssignment(lhsType, rhsType, rhsExpr)
            }
        } else {
            if (lhsType == VexType.UnknownType || rhsType == VexType.UnknownType) {
                return
            }
            val promotedType = VexTypePromotion.promote(lhsType, rhsType, operatorKind)
            if (promotedType == VexType.UnknownType) {
                reporter.reportInvalidOperation(lhsType, rhsType, element)
                return
            }
            if (!VexTypePromotion.isAssignable(lhsType, promotedType)) {
                reporter.reportIncompatibleCompoundAssignment(lhsType, promotedType, element)
            }
        }
    }

    private fun isLiteralInitializerList(expr: PsiElement?): Boolean =
        expr is VexPrimaryExpr && expr.vectorLiteral != null
}
