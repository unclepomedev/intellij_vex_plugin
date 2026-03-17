package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.psi.VexExpr
import com.github.unclepomedev.houdinivexassist.psi.VexFile
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionResolver
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.github.unclepomedev.houdinivexassist.types.VexTypePromotion

class VexCallChecker(private val reporter: VexTypeCheckReporter) {

    fun check(element: VexCallExpr) {
        val args = element.argumentList?.exprList ?: return
        val argTypes = args.map(VexTypeInference::inferType)

        val paramTypes = VexFunctionResolver.resolveParameterTypes(element)

        if (paramTypes == null || paramTypes.size != args.size) {
            reportMissingOverloadIfNeeded(element, args.size)
            return
        }

        reportTypeMismatches(args, argTypes, paramTypes)
    }

    private fun reportMissingOverloadIfNeeded(element: VexCallExpr, argCount: Int) {
        val funcName = element.identifier.text
        val file = element.containingFile as? VexFile ?: return

        if (VexFunctionResolver.isKnownFunction(funcName, file)) {
            reporter.reportMissingOverload(funcName, argCount, element.identifier)
        }
    }

    private fun reportTypeMismatches(
        args: List<VexExpr>,
        argTypes: List<VexType>,
        paramTypes: List<VexType>
    ) {
        for (i in args.indices) {
            if (i >= paramTypes.size) break

            val expected = paramTypes[i]
            val actual = argTypes[i]

            if (isTypeCheckBypassed(expected, actual)) continue

            if (!VexTypePromotion.isAssignable(expected, actual)) {
                val argumentPosition = i + 1
                reporter.reportArgumentTypeMismatch(expected, actual, argumentPosition, args[i])
            }
        }
    }

    private fun isTypeCheckBypassed(expected: VexType, actual: VexType): Boolean {
        return expected == VexType.UnknownType || actual == VexType.UnknownType
    }
}
