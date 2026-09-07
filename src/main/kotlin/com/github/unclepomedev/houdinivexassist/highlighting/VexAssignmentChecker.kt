package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.VexAssignExpr
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion
import com.github.unclepomedev.houdinivexassist.types.operatorKind

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
            if (
                lhsType is VexType.ArrayType &&
                    VexTypePromotion.isArrayLiteralAssignable(lhsType, rhsExpr)
            ) {
                return
            }
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
}
