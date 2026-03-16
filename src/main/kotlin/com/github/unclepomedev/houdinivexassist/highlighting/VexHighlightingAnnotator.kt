package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement

class VexHighlightingAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexCallExpr -> highlightCallExpr(element, holder)
            is VexFunctionDef -> highlightFunctionDef(element, holder)
            is VexStructDef -> highlightStructDef(element, holder)
        }
    }

    private fun highlightCallExpr(element: VexCallExpr, holder: AnnotationHolder) {
        val identifier = element.identifier
        val containingFile = element.containingFile as? VexFile ?: return

        if (VexFunctionResolver.isKnownFunction(identifier.text, containingFile)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(identifier.textRange)
                .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_CALL)
                .create()
        }
    }

    private fun highlightFunctionDef(element: VexFunctionDef, holder: AnnotationHolder) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.identifier.textRange)
            .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
            .create()
    }

    private fun highlightStructDef(element: VexStructDef, holder: AnnotationHolder) {
        val identifier = element.identifier ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(identifier.textRange)
            .textAttributes(DefaultLanguageHighlighterColors.CLASS_NAME)
            .create()
    }
}
