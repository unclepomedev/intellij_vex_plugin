package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexFunctionResolver
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
        return VexFunctionResolver.resolveFunction(element, name, arity)
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }
}
