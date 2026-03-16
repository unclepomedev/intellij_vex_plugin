package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VexTypeInference {

    /**
     * Evaluates any expression (Expr) and infers the resulting VexType.
     */
    fun inferType(expr: PsiElement?): VexType {
        if (expr == null) return VexType.UnknownType

        return when (expr) {
            is VexPrimaryExpr -> inferPrimaryExpr(expr)
            is VexCallExpr -> inferCallExpr(expr)
            else -> VexType.UnknownType
        }
    }

    /**
     * Infer types of PrimaryExpr (literals, variables, parenthetical expressions, vector literals, etc.).
     */
    private fun inferPrimaryExpr(expr: VexPrimaryExpr): VexType {
        // Expressions enclosed in parentheses -> evaluated recursively.
        if (expr.text.startsWith("(") && expr.text.endsWith(")")) {
            val innerExpr = PsiTreeUtil.findChildOfAnyType(expr, VexExpr::class.java)
            if (innerExpr != null && innerExpr !== expr) {
                return inferType(innerExpr)
            }
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

        return if (resolvedDecl != null) {
            VexTypeExtractor.extractType(resolvedDecl)
        } else {
            VexType.UnknownType
        }
    }

    /**
     * infer type of CallExpr
     */
    private fun inferCallExpr(expr: VexCallExpr): VexType {
        val funcName = expr.identifier.text ?: return VexType.UnknownType

        val arity = expr.argumentList?.exprList?.size ?: 0
        val resolvedFunc = VexFunctionResolver.resolveFunction(expr, funcName, arity)
        if (resolvedFunc is VexFunctionDef) {
            return VexTypeExtractor.extractType(resolvedFunc)
        }
        return VexType.UnknownType
    }

    /**
     * A helper that infers the type from the prefix of an attribute (e.g., @P, f@mass).
     */
    private fun inferAttributeType(attrText: String): VexType {
        if (attrText.startsWith("f@")) return VexType.FloatType
        if (attrText.startsWith("v@")) return VexType.VectorType
        if (attrText.startsWith("i@")) return VexType.IntType
        if (attrText.startsWith("s@")) return VexType.StringType
        if (attrText.startsWith("p@")) return VexType.Vector4Type
        if (attrText.startsWith("m@") || attrText.startsWith("3@")) return VexType.Matrix3Type
        if (attrText.startsWith("4@")) return VexType.MatrixType

        if (attrText == "@P" || attrText == "@N" || attrText == "@Cd" || attrText == "@v") return VexType.VectorType
        if (attrText == "@ptnum" || attrText == "@numpt") return VexType.IntType
        if (attrText == "@Time") return VexType.FloatType

        return VexType.UnknownType
    }
}
