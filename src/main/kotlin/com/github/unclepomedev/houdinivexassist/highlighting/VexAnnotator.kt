package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement

class VexAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexCallExpr -> annotateCallExpr(element, holder)
            is VexFunctionDef -> annotateFunctionDef(element, holder)
            is VexStructDef -> annotateStructDef(element, holder)
            is VexDeclarationItem -> annotateDeclarationItem(element, holder)
            is VexPrimaryExpr -> annotatePrimaryExpr(element, holder)
        }
    }

    /**
     * check variable declarations (for flag duplicate errors)
     */
    private fun annotateDeclarationItem(element: VexDeclarationItem, holder: AnnotationHolder) {
        val identifier = element.identifier
        val varName = identifier.text
        val scope = VexScopeAnalyzer.findDeclarationScope(element) ?: return

        val declarationsInScope = VexScopeAnalyzer.getDeclarationsInScope(scope)
        val hasScopeConflict = declarationsInScope.any { sibling ->
            sibling != element &&
                    sibling.identifier.text == varName &&
                    sibling.textOffset < element.textOffset
        }

        if (hasScopeConflict) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Variable '$varName' is already defined in this scope")
                .range(identifier.textRange)
                .create()
            return
        }

        val parameters = VexScopeAnalyzer.getParametersForScope(scope)
        val hasParamConflict = parameters.any { it.identifier.text == varName }

        if (hasParamConflict) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Variable '$varName' is already defined as a parameter")
                .range(identifier.textRange)
                .create()
        }
    }

    /**
     * check variable usage (for undefined errors)
     */
    private fun annotatePrimaryExpr(element: VexPrimaryExpr, holder: AnnotationHolder) {
        val identifier = element.identifier ?: return
        val varName = identifier.text

        val resolvedElement = VexVariableResolver.resolveVariable(element, varName)

        if (resolvedElement == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved variable: '$varName'")
                .range(identifier.textRange)
                .textAttributes(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
                .create()
        }
    }

    private fun annotateCallExpr(element: VexCallExpr, holder: AnnotationHolder) {
        val identifier = element.identifier
        val functionName = identifier.text

        val apiProvider = element.project.getService(VexApiProvider::class.java)
        val isStandardFunction = apiProvider.hasFunction(functionName)

        val containingFile = element.containingFile as? VexFile ?: return
        val localFunctionNames = VexScopeAnalyzer.getLocalFunctionNames(containingFile)
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
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.identifier.textRange)
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
