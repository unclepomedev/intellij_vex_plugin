package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VexFunctionResolver {
    /**
     * Finds and returns the VexFunctionDef of the specified function name.
     * Returns null if not found.
     */
    fun resolveFunction(element: PsiElement, functionName: String, arity: Int? = null): PsiElement? {
        val file = element.containingFile as? VexFile ?: return null
        val localFunctions = PsiTreeUtil.findChildrenOfType(file, VexFunctionDef::class.java)
        val candidates = localFunctions.filter { it.identifier.text == functionName }
        if (arity == null) return candidates.firstOrNull()
        // TODO: Checking for matching the number of arguments is temporary; once type inference is implemented, we will verify the matching of type signatures.
        return candidates.firstOrNull { def ->
            val paramCount = def.parameterListDef?.parameterDefList?.size ?: 0
            paramCount == arity
        } ?: candidates.firstOrNull()
    }

    /**
     * Check if the specified function name actually exists as a standard function or a local function.
     */
    fun isKnownFunction(functionName: String, file: VexFile): Boolean {
        val apiProvider = file.project.getService(VexApiProvider::class.java)
        if (apiProvider?.hasFunction(functionName) == true) return true

        val localFunctions = VexScopeAnalyzer.getLocalFunctionNames(file)
        return functionName in localFunctions
    }
}
