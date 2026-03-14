package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionDef
import com.github.unclepomedev.houdinivexassist.psi.VexStructDef
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement

class VexAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexCallExpr -> {
                val identifier = element.identifier
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(identifier.textRange)
                    .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_CALL)
                    .create()
            }

            is VexFunctionDef -> {
                val identifier = element.identifier
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(identifier.textRange)
                    .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
                    .create()
            }

            is VexStructDef -> {
                val identifier = element.identifier
                if (identifier != null) {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(identifier.textRange)
                        .textAttributes(DefaultLanguageHighlighterColors.CLASS_NAME)
                        .create()
                }
            }
        }
    }
}
