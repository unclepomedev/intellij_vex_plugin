package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VexTypeInference {

    fun inferType(expr: PsiElement?): VexType {
        if (expr == null) return VexType.UnknownType

        return when (expr) {
            is VexPrimaryExpr -> inferPrimaryExpr(expr)
            is VexCallExpr -> inferCallExpr(expr)
            is VexAddExpr, is VexMulExpr,
            is VexBitwiseAndExpr, is VexBitwiseOrExpr, is VexBitwiseXorExpr, is VexShiftExpr -> inferArithmeticExpr(expr)

            is VexEqualityExpr, is VexRelationalExpr,
            is VexLogicalAndExpr, is VexLogicalOrExpr -> VexType.IntType // Boolean representation in VEX
            is VexAssignExpr -> inferAssignmentExpr(expr)
            else -> VexType.UnknownType
        }
    }

    private fun inferPrimaryExpr(expr: VexPrimaryExpr): VexType {
        if (isParenthesizedExpr(expr)) {
            return inferParenthesizedExpr(expr)
        }

        if (expr.vectorLiteral != null) return VexType.VectorType

        val node = expr.node
        return when {
            node.findChildByType(VexTypes.NUMBER) != null -> inferNumberLiteral(expr.text)
            node.findChildByType(VexTypes.STRING) != null ||
                    node.findChildByType(VexTypes.UNCLOSED_STRING) != null -> VexType.StringType

            node.findChildByType(VexTypes.ATTRIBUTE) != null -> inferAttributeType(expr.text)
            expr.identifier != null -> inferVariableReference(expr)
            else -> VexType.UnknownType
        }
    }

    private fun isParenthesizedExpr(expr: VexPrimaryExpr): Boolean {
        return expr.text.startsWith("(") && expr.text.endsWith(")")
    }

    private fun inferParenthesizedExpr(expr: VexPrimaryExpr): VexType {
        val innerExpr = PsiTreeUtil.findChildOfAnyType(expr, VexExpr::class.java)
        return if (innerExpr != null && innerExpr !== expr) {
            inferType(innerExpr)
        } else {
            VexType.UnknownType
        }
    }

    private fun inferNumberLiteral(text: String): VexType {
        return if (text.contains(".") || text.contains("e", ignoreCase = true)) {
            VexType.FloatType
        } else {
            VexType.IntType
        }
    }

    private fun inferVariableReference(expr: VexPrimaryExpr): VexType {
        val varName = expr.identifier?.text ?: return VexType.UnknownType
        val resolvedDecl = VexVariableResolver.resolveVariable(expr, varName)
        return resolvedDecl?.let { VexTypeExtractor.extractType(it) } ?: VexType.UnknownType
    }

    private fun inferCallExpr(expr: VexCallExpr): VexType {
        val funcName = expr.identifier.text ?: return VexType.UnknownType
        val arity = expr.argumentList?.exprList?.size ?: 0
        val resolvedFunc = VexFunctionResolver.resolveFunction(expr, funcName, arity)

        return if (resolvedFunc is VexFunctionDef) {
            VexTypeExtractor.extractType(resolvedFunc)
        } else {
            VexType.UnknownType
        }
    }

    private fun inferAttributeType(attrText: String): VexType {
        if (attrText.startsWith("f@")) return VexType.FloatType
        if (attrText.startsWith("v@")) return VexType.VectorType
        if (attrText.startsWith("i@")) return VexType.IntType
        if (attrText.startsWith("s@")) return VexType.StringType
        if (attrText.startsWith("p@")) return VexType.Vector4Type
        if (attrText.startsWith("m@") || attrText.startsWith("3@")) return VexType.Matrix3Type
        if (attrText.startsWith("4@")) return VexType.MatrixType

        return when (attrText) {
            "@P", "@N", "@Cd", "@v" -> VexType.VectorType
            "@ptnum", "@numpt" -> VexType.IntType
            "@Time" -> VexType.FloatType
            else -> VexType.UnknownType
        }
    }

    private fun inferArithmeticExpr(expr: PsiElement): VexType {
        val (left, right) = expr.binaryOperands ?: return VexType.UnknownType

        val operatorKind = expr.operatorKind ?: when (expr) {
            is VexMulExpr -> VexTypePromotion.OperatorKind.MULTIPLICATIVE
            is VexBitwiseAndExpr, is VexBitwiseOrExpr, is VexBitwiseXorExpr -> VexTypePromotion.OperatorKind.BITWISE
            is VexShiftExpr -> VexTypePromotion.OperatorKind.SHIFT
            else -> return VexType.UnknownType
        }

        return VexTypePromotion.promote(inferType(left), inferType(right), operatorKind)
    }

    private fun inferAssignmentExpr(expr: VexAssignExpr): VexType {
        val (left, right) = expr.binaryOperands ?: return VexType.UnknownType

        val leftType = inferType(left)
        val rightType = inferType(right)

        if (leftType == VexType.UnknownType || rightType == VexType.UnknownType) {
            return VexType.UnknownType
        }

        val kind = expr.operatorKind
        if (kind != null) {
            if (VexTypePromotion.promote(leftType, rightType, kind) == VexType.UnknownType) {
                return VexType.UnknownType
            }
        }

        return leftType
    }
}
