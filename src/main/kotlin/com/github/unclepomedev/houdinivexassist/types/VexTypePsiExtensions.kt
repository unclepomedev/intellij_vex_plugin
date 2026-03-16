package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.psi.VexAddExpr
import com.github.unclepomedev.houdinivexassist.psi.VexExpr
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.psi.PsiElement

/**
 * Retrieves two VexExpr objects, the left and right sides, from any expression node.
 * Returns null if there are not two matching elements.
 */
internal val PsiElement.binaryOperands: Pair<VexExpr, VexExpr>?
    get() {
        val operands = children.filterIsInstance<VexExpr>()
        return if (operands.size == 2) Pair(operands[0], operands[1]) else null
    }

/**
 * From the expression node, obtain the OperatorKind corresponding to the operator token it possesses.
 */
internal val PsiElement.operatorKind: VexTypePromotion.OperatorKind?
    get() {
        if (this is VexAddExpr) {
            if (node.findChildByType(VexTypes.PLUS) != null) return VexTypePromotion.OperatorKind.ADDITIVE
            if (node.findChildByType(VexTypes.MINUS) != null) return VexTypePromotion.OperatorKind.SUBTRACTIVE
            return null
        }

        val opNode = node.getChildren(null).firstOrNull {
            VexTypePromotion.getOperatorKind(it.elementType) != null
        }
        return opNode?.let { VexTypePromotion.getOperatorKind(it.elementType) }
    }
