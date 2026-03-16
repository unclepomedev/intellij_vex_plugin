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
            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved variable: '$varName'")
                .range(identifier.textRange)
                .create()
        }
    }

    private fun checkFunctionCall(element: VexCallExpr, holder: AnnotationHolder) {
        val identifier = element.identifier
        val containingFile = element.containingFile as? VexFile ?: return

        if (!VexFunctionResolver.isKnownFunction(identifier.text, containingFile)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unknown VEX function: '${identifier.text}'")
                .range(identifier.textRange)
                .create()
        }
    }
}
