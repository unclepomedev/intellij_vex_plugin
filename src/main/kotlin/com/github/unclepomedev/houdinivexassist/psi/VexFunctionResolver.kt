package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VexFunctionResolver {
    /**
     * Finds and returns the VexFunctionDef of the specified function name.
     * Returns null if not found.
     */
    fun resolveFunction(element: PsiElement, functionName: String): PsiElement? {
        val file = element.containingFile as? VexFile ?: return null
        val localFunctions = PsiTreeUtil.findChildrenOfType(file, VexFunctionDef::class.java)
        return localFunctions.find { it.identifier.text == functionName }
    }
}
