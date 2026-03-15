package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionDef
import com.github.unclepomedev.houdinivexassist.psi.VexStructDef
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil

class VexAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexCallExpr -> annotateCallExpr(element, holder)
            is VexFunctionDef -> annotateFunctionDef(element, holder)
            is VexStructDef -> annotateStructDef(element, holder)
        }
    }

    private fun annotateCallExpr(element: VexCallExpr, holder: AnnotationHolder) {
        val identifier = element.identifier
        val functionName = identifier.text

        val apiProvider = element.project.getService(VexApiProvider::class.java)
        val isStandardFunction = apiProvider.hasFunction(functionName)

        val containingFile = element.containingFile
        val localFunctionNames = CachedValuesManager.getCachedValue(containingFile) {
            val names = PsiTreeUtil.findChildrenOfType(containingFile, VexFunctionDef::class.java)
                .mapNotNull { it.identifier.text }
                .toSet()
            CachedValueProvider.Result.create(names, containingFile)
        }

        val isLocalFunction = functionName in localFunctionNames

        if (isStandardFunction || isLocalFunction) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(identifier.textRange)
                .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_CALL)
                .create()
        } else {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unknown VEX function: '$functionName'")
                .range(identifier.textRange)
                .create()
        }
    }

    private fun annotateFunctionDef(element: VexFunctionDef, holder: AnnotationHolder) {
        val identifier = element.identifier

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(identifier.textRange)
            .textAttributes(DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
            .create()
    }

    private fun annotateStructDef(element: VexStructDef, holder: AnnotationHolder) {
        val identifier = element.identifier ?: return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(identifier.textRange)
            .textAttributes(DefaultLanguageHighlighterColors.CLASS_NAME)
            .create()
    }
}
