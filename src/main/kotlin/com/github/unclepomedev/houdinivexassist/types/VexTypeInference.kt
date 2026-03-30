package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VexTypeInference {

    fun inferType(expr: PsiElement?): VexType {
        if (expr == null) return VexType.UnknownType

        return when (expr) {
            is VexAttributeExpr -> (expr as? com.github.unclepomedev.houdinivexassist.psi.impl.VexAttributeExprMixin)?.inferType()
                ?: VexType.UnknownType

            is VexPrimaryExpr -> inferPrimaryExpr(expr)
            is VexCallExpr -> inferCallExpr(expr)
            is VexAddExpr, is VexMulExpr,
            is VexBitwiseAndExpr, is VexBitwiseOrExpr, is VexBitwiseXorExpr, is VexShiftExpr -> inferArithmeticExpr(expr)

            is VexEqualityExpr, is VexRelationalExpr,
            is VexLogicalAndExpr, is VexLogicalOrExpr -> VexType.IntType // Boolean representation in VEX
            is VexAssignExpr -> inferAssignmentExpr(expr)
            is VexMemberExpr -> inferMemberExpr(expr)

            is VexCastExpr -> inferCastExpr(expr)
            is VexTypeCastCallExpr -> inferTypeCastCallExpr(expr)
            is VexArrayAccessExpr -> inferArrayAccessExpr(expr)

            is VexPrefixExpr -> inferPrefixExpr(expr)
            is VexPostfixExpr -> inferPostfixExpr(expr)

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

    private fun inferMemberExpr(expr: VexMemberExpr): VexType {
        val baseExpr = expr.expr
        val memberName = expr.identifier?.text ?: return VexType.UnknownType

        val baseType = inferType(baseExpr)

        if (baseType is VexType.StructType) {
            return resolveStructMemberType(expr, baseType.name, memberName)
        }

        val isSwizzlable = baseType == VexType.Vector2Type ||
                baseType == VexType.VectorType ||
                baseType == VexType.Vector4Type ||
                baseType == VexType.Matrix3Type ||
                baseType == VexType.MatrixType

        if (isSwizzlable) {
            val validSwizzleChars = setOf('x', 'y', 'z', 'w', 'r', 'g', 'b', 'a', 'u', 'v')
            if (memberName.isEmpty() || memberName.any { it !in validSwizzleChars }) {
                return VexType.UnknownType
            }
            return when (memberName.length) {
                1 -> VexType.FloatType   // .x, .r, .u
                2 -> VexType.Vector2Type // .xy, .uv
                3 -> VexType.VectorType  // .xyz, .rgb
                4 -> VexType.Vector4Type // .xyzw, .rgba
                else -> VexType.UnknownType
            }
        }
        return VexType.UnknownType
    }

    private fun resolveStructMemberType(context: PsiElement, structName: String, memberName: String): VexType {
        val structDefs = VexScopeAnalyzer.getVisibleStructs(context)

        val targetStruct = structDefs.find { it.identifier?.text == structName } ?: return VexType.UnknownType

        for (member in targetStruct.structMemberList) {
            val matchedDecl = member.declarationItemList.find { it.identifier.text == memberName }
            if (matchedDecl != null) {
                val typeString = member.typeRef.text ?: return VexType.UnknownType
                return VexType.fromString(typeString)
            }
        }

        return VexType.UnknownType
    }

    private fun inferCastExpr(expr: VexCastExpr): VexType {
        val typeName = expr.typeRef.text ?: return VexType.UnknownType
        return VexType.fromString(typeName)
    }

    private fun inferTypeCastCallExpr(expr: VexTypeCastCallExpr): VexType {
        val node = expr.node
        return when {
            node.findChildByType(VexTypes.FLOAT_KW) != null -> VexType.FloatType
            node.findChildByType(VexTypes.INT_KW) != null -> VexType.IntType
            node.findChildByType(VexTypes.VECTOR_KW) != null -> VexType.VectorType
            node.findChildByType(VexTypes.VECTOR2_KW) != null -> VexType.Vector2Type
            node.findChildByType(VexTypes.VECTOR4_KW) != null -> VexType.Vector4Type
            node.findChildByType(VexTypes.MATRIX_KW) != null -> VexType.MatrixType
            node.findChildByType(VexTypes.MATRIX3_KW) != null -> VexType.Matrix3Type
            node.findChildByType(VexTypes.STRING_KW) != null -> VexType.StringType
            node.findChildByType(VexTypes.DICT_KW) != null -> VexType.DictType
            else -> VexType.UnknownType
        }
    }

    private fun inferArrayAccessExpr(expr: VexArrayAccessExpr): VexType {
        val arrayExpr = expr.exprList.firstOrNull() ?: return VexType.UnknownType
        return when (val baseType = inferType(arrayExpr)) {
            is VexType.ArrayType -> baseType.elementType
            VexType.MatrixType -> VexType.Vector4Type
            VexType.Matrix3Type -> VexType.VectorType
            VexType.VectorType, VexType.Vector2Type, VexType.Vector4Type -> VexType.FloatType

            else -> VexType.UnknownType
        }
    }

    private fun inferPrefixExpr(expr: VexPrefixExpr): VexType {
        val operand = expr.children.firstOrNull { it is VexExpr } ?: return VexType.UnknownType

        if (expr.node.findChildByType(VexTypes.NOT) != null ||
            expr.node.findChildByType(VexTypes.BITNOT) != null
        ) {
            return VexType.IntType
        }
        return inferType(operand)
    }

    private fun inferPostfixExpr(expr: VexPostfixExpr): VexType {
        val operand = expr.children.firstOrNull { it is VexExpr } ?: return VexType.UnknownType
        return inferType(operand)
    }
}
