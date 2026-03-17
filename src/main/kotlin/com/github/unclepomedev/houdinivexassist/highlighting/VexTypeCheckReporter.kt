package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.types.VexType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class VexTypeCheckReporter(private val holder: AnnotationHolder) {

    fun reportIncompatibleAssignment(lhsType: VexType, rhsType: VexType, expr: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Incompatible types: cannot assign '${rhsType.displayName}' to '${lhsType.displayName}'"
        ).range(expr.textRange).create()
    }

    fun reportInvalidOperation(lhsType: VexType, rhsType: VexType, element: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Invalid operation: cannot apply operator to '${lhsType.displayName}' and '${rhsType.displayName}'"
        ).range(element.textRange).create()
    }

    fun reportIncompatibleCompoundAssignment(lhsType: VexType, promotedType: VexType, element: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Incompatible types: cannot assign result of type '${promotedType.displayName}' to '${lhsType.displayName}'"
        ).range(element.textRange).create()
    }

    fun reportMissingOverload(funcName: String, argCount: Int, element: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "No matching overload for function '$funcName' with $argCount arguments"
        ).range(element.textRange).create()
    }

    fun reportArgumentTypeMismatch(expected: VexType, actual: VexType, position: Int, expr: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Type mismatch in argument $position: expected '${expected.displayName}', got '${actual.displayName}'"
        ).range(expr.textRange).create()
    }

    fun reportUnexpectedReturnValue(expr: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Cannot return a value from a function returning 'void'"
        ).range(expr.textRange).create()
    }

    fun reportMissingReturnValue(expectedType: VexType, element: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Missing return value: expected '${expectedType.displayName}'"
        ).range(element.textRange).create()
    }

    fun reportIncompatibleReturnType(expectedType: VexType, actualType: VexType, expr: PsiElement) {
        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Incompatible return type: expected '${expectedType.displayName}', got '${actualType.displayName}'"
        ).range(expr.textRange).create()
    }
}
