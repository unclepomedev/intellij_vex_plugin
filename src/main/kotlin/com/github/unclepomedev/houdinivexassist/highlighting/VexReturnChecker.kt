package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.VexFunctionDef
import com.github.unclepomedev.houdinivexassist.psi.VexReturnStatement
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeExtractor
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion
import com.intellij.psi.util.PsiTreeUtil

class VexReturnChecker(private val reporter: VexTypeCheckReporter) {

    fun check(element: VexReturnStatement) {
        val enclosingFunction = PsiTreeUtil.getParentOfType(element, VexFunctionDef::class.java)
            ?: return // Not inside a function

        val declaredReturnType = VexTypeExtractor.extractType(enclosingFunction)
        if (declaredReturnType == VexType.UnknownType) return

        val returnExpr = element.expr
        val isVoidFunction = declaredReturnType.displayName == "void"

        if (isVoidFunction) {
            if (returnExpr != null) {
                reporter.reportUnexpectedReturnValue(returnExpr)
            }
            return
        }

        if (returnExpr == null) {
            reporter.reportMissingReturnValue(declaredReturnType, element)
            return
        }

        val actualReturnType = VexTypeInference.inferType(returnExpr)
        if (actualReturnType == VexType.UnknownType) return

        if (!VexTypePromotion.isAssignable(declaredReturnType, actualReturnType)) {
            reporter.reportIncompatibleReturnType(declaredReturnType, actualReturnType, returnExpr)
        }
    }
}
