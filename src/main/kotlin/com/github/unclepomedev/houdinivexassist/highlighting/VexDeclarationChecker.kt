package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.VexDeclarationItem
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeExtractor
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion

class VexDeclarationChecker(private val reporter: VexTypeCheckReporter) {

    fun check(element: VexDeclarationItem) {
        val expr = element.expr ?: return
        val declaredType = VexTypeExtractor.extractType(element)

        if (
            declaredType is VexType.ArrayType &&
                VexTypePromotion.isArrayLiteralAssignable(declaredType, expr)
        ) {
            return
        }

        val inferredType = VexTypeInference.inferType(expr)

        if (!VexTypePromotion.isAssignable(declaredType, inferredType)) {
            reporter.reportIncompatibleAssignment(declaredType, inferredType, expr)
        }
    }
}
