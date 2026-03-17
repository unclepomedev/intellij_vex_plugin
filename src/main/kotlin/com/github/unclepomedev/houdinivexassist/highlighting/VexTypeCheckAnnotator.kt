package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.types.*
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion.OperatorKind
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
            checkSimpleAssignment(lhsType, rhsType, rhsExpr, holder)
        } else {
            checkCompoundAssignment(lhsType, rhsType, operatorKind, element, holder)
        }
    }

    private fun checkSimpleAssignment(
        lhsType: VexType,
        rhsType: VexType,
        rhsExpr: PsiElement,
        holder: AnnotationHolder
    ) {
        if (lhsType is VexType.ArrayType &&
            isLiteralInitializerList(rhsExpr) &&
            rhsType == VexType.UnknownType
        ) {
            return
        }

        if (!VexTypePromotion.isAssignable(lhsType, rhsType)) {
            reportIncompatibleAssignment(lhsType, rhsType, rhsExpr, holder)
        }
    }

    private fun checkCompoundAssignment(
        lhsType: VexType,
        rhsType: VexType,
        operatorKind: OperatorKind,
        element: VexAssignExpr,
        holder: AnnotationHolder
    ) {
        val promotedType = VexTypePromotion.promote(lhsType, rhsType, operatorKind)

        if (promotedType == VexType.UnknownType) {
            reportInvalidOperation(lhsType, rhsType, element, holder)
            return
        }

        if (!VexTypePromotion.isAssignable(lhsType, promotedType)) {
            reportIncompatibleCompoundAssignment(lhsType, promotedType, element, holder)
        }
    }

    private fun reportIncompatibleAssignment(
        lhsType: VexType,
        rhsType: VexType,
        rhsExpr: PsiElement,
        holder: AnnotationHolder
    ) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Incompatible types: cannot assign '${rhsType.displayName}' to '${lhsType.displayName}'"
        )
            .range(rhsExpr.textRange)
            .create()
    }

    private fun reportInvalidOperation(
        lhsType: VexType,
        rhsType: VexType,
        element: PsiElement,
        holder: AnnotationHolder
    ) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Invalid operation: cannot apply operator to '${lhsType.displayName}' and '${rhsType.displayName}'"
        )
            .range(element.textRange)
            .create()
    }

    private fun reportIncompatibleCompoundAssignment(
        lhsType: VexType,
        promotedType: VexType,
        element: PsiElement,
        holder: AnnotationHolder
    ) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Incompatible types: cannot assign result of type '${promotedType.displayName}' to '${lhsType.displayName}'"
        )
            .range(element.textRange)
            .create()
    }

    private fun checkFunctionArguments(element: VexCallExpr, holder: AnnotationHolder) {
        val args = element.argumentList?.exprList ?: return
        val argTypes = args.map(VexTypeInference::inferType)

        val paramTypes = VexFunctionResolver.resolveParameterTypes(element)

        if (paramTypes == null || paramTypes.size != args.size) {
            reportMissingOverloadIfNeeded(element, args.size, holder)
            return
        }

        reportTypeMismatches(args, argTypes, paramTypes, holder)
    }

    private fun reportMissingOverloadIfNeeded(element: VexCallExpr, argCount: Int, holder: AnnotationHolder) {
        val funcName = element.identifier.text
        val file = element.containingFile as? VexFile ?: return

        if (VexFunctionResolver.isKnownFunction(funcName, file)) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "No matching overload for function '$funcName' with $argCount arguments"
            )
                .range(element.identifier.textRange)
                .create()
        }
    }

    private fun reportTypeMismatches(
        args: List<VexExpr>,
        argTypes: List<VexType>,
        paramTypes: List<VexType>,
        holder: AnnotationHolder
    ) {
        for (i in args.indices) {
            if (i >= paramTypes.size) break

            val expected = paramTypes[i]
            val actual = argTypes[i]

            if (isTypeCheckBypassed(expected, actual)) continue

            if (!VexTypePromotion.isAssignable(expected, actual)) {
                val argumentPosition = i + 1
                reportArgumentTypeMismatch(args[i], expected, actual, argumentPosition, holder)
            }
        }
    }

    private fun isTypeCheckBypassed(expected: VexType, actual: VexType): Boolean {
        return expected == VexType.UnknownType || actual == VexType.UnknownType
    }

    private fun reportArgumentTypeMismatch(
        argExpr: VexExpr, // Assuming element.argumentList?.exprList returns List<VexExpr>
        expected: VexType,
        actual: VexType,
        argumentPosition: Int,
        holder: AnnotationHolder
    ) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Type mismatch in argument $argumentPosition: expected '${expected.displayName}', got '${actual.displayName}'"
        )
            .range(argExpr.textRange)
            .create()
    }

    /**
     * Determines whether the expression is a pure {...} literal (initializer list).
     */
    private fun isLiteralInitializerList(expr: PsiElement?): Boolean {
        return expr is VexPrimaryExpr && expr.vectorLiteral != null
    }
}
