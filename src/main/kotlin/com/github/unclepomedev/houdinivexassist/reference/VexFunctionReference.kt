package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionResolver
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VexFunctionReference(
    element: PsiElement,
    textRange: TextRange,
    private val name: String,
    private val arity: Int
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        val callExpr = (element as? VexCallExpr) ?: (element.parent as? VexCallExpr)

        if (callExpr != null) {
            val args = callExpr.argumentList?.exprList ?: emptyList()
            val argTypes = args.map(VexTypeInference::inferType)

            return VexFunctionResolver.resolveFunction(element, name, args.size, argTypes)
        }

        // Fallback in case CallExpr cannot be obtained.
        return VexFunctionResolver.resolveFunction(element, name, arity)
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }
}
