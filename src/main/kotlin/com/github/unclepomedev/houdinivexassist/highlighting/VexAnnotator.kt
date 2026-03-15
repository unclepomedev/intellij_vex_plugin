package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
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
            is VexDeclarationItem -> annotateDeclarationItem(element, holder)
        }
    }

    private fun findDeclarationScope(element: PsiElement): PsiElement? =
        PsiTreeUtil.getParentOfType(
            element,
            VexBlock::class.java,
            VexStructDef::class.java,
            VexFile::class.java
        )

    private fun annotateDeclarationItem(element: VexDeclarationItem, holder: AnnotationHolder) {
        val identifier = element.identifier
        val varName = identifier.text
        val scope = findDeclarationScope(element) ?: return

        if (checkScopeConflict(element, varName, scope, holder)) return
        checkParameterConflict(element, varName, scope, holder)
    }

    private fun checkScopeConflict(
        element: VexDeclarationItem,
        varName: String,
        scope: PsiElement,
        holder: AnnotationHolder
    ): Boolean {
        val declarationsInScope = CachedValuesManager.getCachedValue(scope) {
            val decls = PsiTreeUtil.findChildrenOfType(scope, VexDeclarationItem::class.java)
                .filter { findDeclarationScope(it) == scope }
            CachedValueProvider.Result.create(decls, scope)
        }

        // Find one that was declared before it and have the same direct parent scope.
        val hasConflict = declarationsInScope.any { sibling ->
            sibling != element &&
                    sibling.identifier.text == varName &&
                    sibling.textOffset < element.textOffset
        }

        if (hasConflict) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Variable '$varName' is already defined in this scope")
                .range(element.identifier.textRange)
                .create()
            return true
        }
        return false
    }

    private fun checkParameterConflict(
        element: VexDeclarationItem,
        varName: String,
        scope: PsiElement,
        holder: AnnotationHolder
    ) {
        // No check is necessary if it's not a block directly under a function.
        if (scope !is VexBlock || scope.parent !is VexFunctionDef) return

        val funcDef = scope.parent as VexFunctionDef
        val paramList = funcDef.parameterListDef ?: return

        val parameters = CachedValuesManager.getCachedValue(paramList) {
            val params = PsiTreeUtil.findChildrenOfType(paramList, VexParameterDef::class.java)
            CachedValueProvider.Result.create(params, paramList)
        }

        val hasConflict = parameters.any { it.identifier.text == varName }

        if (hasConflict) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Variable '$varName' is already defined as a parameter")
                .range(element.identifier.textRange)
                .create()
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
