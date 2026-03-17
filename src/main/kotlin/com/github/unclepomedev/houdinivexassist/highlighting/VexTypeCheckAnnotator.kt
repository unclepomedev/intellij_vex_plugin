package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.types.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class VexTypeCheckAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexDeclarationItem -> checkDeclarationInitialization(element, holder)
            is VexAssignExpr -> checkAssignmentExpression(element, holder)
            is VexCallExpr -> checkFunctionArguments(element, holder)
        }
    }

    private fun checkDeclarationInitialization(element: VexDeclarationItem, holder: AnnotationHolder) {
        val expr = element.expr ?: return
        val declaredType = VexTypeExtractor.extractType(element)

        if (declaredType is VexType.ArrayType && isLiteralInitializerList(expr)) {
            return
        }

        val inferredType = VexTypeInference.inferType(expr)

        if (!VexTypePromotion.isAssignable(declaredType, inferredType)) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Incompatible types: cannot assign '${inferredType.displayName}' to '${declaredType.displayName}'"
            )
                .range(expr.textRange)
                .create()
        }
    }

    private fun checkAssignmentExpression(element: VexAssignExpr, holder: AnnotationHolder) {
        val exprs = element.exprList
        if (exprs.size < 2) return

        val lhsExpr = exprs[0]
        val rhsExpr = exprs[1]

        val lhsType = VexTypeInference.inferType(lhsExpr)
        val rhsType = VexTypeInference.inferType(rhsExpr)
        val operatorKind = element.operatorKind

        if (operatorKind == null) {
            if (lhsType is VexType.ArrayType && isLiteralInitializerList(rhsExpr)) {
                return
            }
            if (!VexTypePromotion.isAssignable(lhsType, rhsType)) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Incompatible types: cannot assign '${rhsType.displayName}' to '${lhsType.displayName}'"
                )
                    .range(rhsExpr.textRange)
                    .create()
            }
            return
        }

        val promotedType = VexTypePromotion.promote(lhsType, rhsType, operatorKind)

        if (promotedType == VexType.UnknownType) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Invalid operation: cannot apply operator to '${lhsType.displayName}' and '${rhsType.displayName}'"
            )
                .range(element.textRange)
                .create()
            return
        }

        if (!VexTypePromotion.isAssignable(lhsType, promotedType)) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Incompatible types: cannot assign result of type '${promotedType.displayName}' to '${lhsType.displayName}'"
            )
                .range(element.textRange)
                .create()
        }
    }

    private fun checkFunctionArguments(element: VexCallExpr, holder: AnnotationHolder) {
        val args = element.argumentList?.exprList ?: return
        val argTypes = args.map { VexTypeInference.inferType(it) }

        val paramTypes = VexFunctionResolver.resolveParameterTypes(element)
            ?: return // unknown function or can't resolve — skip

        for (i in args.indices) {
            if (i >= paramTypes.size) break
            val expected = paramTypes[i]
            val actual = argTypes[i]
            if (expected == VexType.UnknownType || actual == VexType.UnknownType) continue
            if (!VexTypePromotion.isAssignable(expected, actual)) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Type mismatch in argument ${i + 1}: expected '${expected.displayName}', got '${actual.displayName}'"
                )
                    .range(args[i].textRange)
                    .create()
            }
        }
    }

    /**
     * Determines whether the expression is a pure {...} literal (initializer list).
     */
    private fun isLiteralInitializerList(expr: PsiElement?): Boolean {
        return expr is VexPrimaryExpr && expr.vectorLiteral != null
    }
}
