package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class VexReferenceAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexPrimaryExpr -> checkVariableUsage(element, holder)
            is VexCallExpr -> checkFunctionCall(element, holder)
        }
    }

    private fun checkVariableUsage(element: VexPrimaryExpr, holder: AnnotationHolder) {
        val identifier = element.identifier ?: return
        val varName = identifier.text

        val resolvedElement = VexVariableResolver.resolveVariable(element, varName)

        if (resolvedElement == null) {
            val resolvedMacro = VexMacroResolver.resolveMacro(element, varName)
            if (resolvedMacro != null) return

            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved variable: '$varName'").range(identifier.textRange)
                .create()
            return
        }
        if (resolvedElement is VexDeclarationItem) {
            // Check whether the variable on the right-hand side is within the tree of its own declaration statement.
            if (com.intellij.psi.util.PsiTreeUtil.isAncestor(resolvedElement, element, false)) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Variable '$varName' is used in its own initialization")
                    .range(identifier.textRange).create()
            }
        }
    }

    private fun checkFunctionCall(element: VexCallExpr, holder: AnnotationHolder) {
        val identifier = element.identifier
        val funcName = identifier.text
        val containingFile = element.containingFile as? VexFile ?: return

        if (VexMacroResolver.resolveMacro(element, funcName) != null) return

        // If a variable with the same name is resolved, the function call is invalid (shadowed)
        // However, skip if the call is inside the variable's own initializer (e.g., float dot = dot())
        val resolvedVar = VexVariableResolver.resolveVariable(element, funcName)
        if (resolvedVar != null && !com.intellij.psi.util.PsiTreeUtil.isAncestor(resolvedVar, element, false)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Variable '$funcName' cannot be called as a function")
                .range(identifier.textRange).create()
            return
        }

        if (!VexFunctionResolver.isKnownFunction(funcName, containingFile)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unknown VEX function: '$funcName'")
                .range(identifier.textRange).create()
        }
    }
}
