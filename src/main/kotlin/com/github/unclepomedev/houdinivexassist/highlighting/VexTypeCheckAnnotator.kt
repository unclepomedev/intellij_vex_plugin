package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.VexAssignExpr
import com.github.unclepomedev.houdinivexassist.psi.VexDeclarationItem
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeExtractor
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion
import com.github.unclepomedev.houdinivexassist.types.operatorKind
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class VexTypeCheckAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexDeclarationItem -> checkDeclarationInitialization(element, holder)
            is VexAssignExpr -> checkAssignmentExpression(element, holder)
        }
    }

    private fun checkDeclarationInitialization(element: VexDeclarationItem, holder: AnnotationHolder) {
        val expr = element.expr ?: return

        val declaredType = VexTypeExtractor.extractType(element)

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
}
