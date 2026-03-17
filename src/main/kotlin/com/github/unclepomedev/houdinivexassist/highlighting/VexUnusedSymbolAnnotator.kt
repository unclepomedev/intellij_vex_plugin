package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class VexUnusedSymbolAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexDeclarationItem -> checkUnusedVariable(element, holder)
            is VexFunctionDef -> checkUnusedFunction(element, holder)
            is VexParameterDef -> checkUnusedParameter(element, holder)
        }
    }

    private fun checkUnusedVariable(element: VexDeclarationItem, holder: AnnotationHolder) {
        val identifier = element.identifier
        val varName = identifier.text

        val isStructField = PsiTreeUtil.getParentOfType(element, VexStructMember::class.java) != null

        val isUsed = if (isStructField) {
            val file = element.containingFile ?: return
            val memberAccesses = PsiTreeUtil.findChildrenOfType(file, VexMemberExpr::class.java)
            memberAccesses.any { it.identifier?.text == varName }
        } else {
            val scope = VexScopeAnalyzer.findDeclarationScope(element) ?: return
            val usages = PsiTreeUtil.findChildrenOfType(scope, VexPrimaryExpr::class.java)
            usages.any { expr ->
                expr.identifier?.text == varName && VexVariableResolver.resolveVariable(expr, varName) == element
            }
        }

        if (!isUsed) {
            val messageType = if (isStructField) "field" else "variable"
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Unused $messageType '$varName'")
                .range(identifier.textRange)
                .textAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
                .create()
        }
    }

    private fun checkUnusedFunction(element: VexFunctionDef, holder: AnnotationHolder) {
        val identifier = element.identifier
        val funcName = identifier.text
        val file = element.containingFile as? VexFile ?: return

        // main is not marked as unused
        if (funcName == "main") return

        val usages = PsiTreeUtil.findChildrenOfType(file, VexCallExpr::class.java)
        val isUsed = usages.any { call ->
            val arity = call.argumentList?.exprList?.size ?: 0
            call.identifier.text == funcName && VexFunctionResolver.resolveFunction(call, funcName, arity) == element
        }

        if (!isUsed) {
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Unused function '$funcName'")
                .range(identifier.textRange)
                .textAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
                .create()
        }
    }

    private fun checkUnusedParameter(element: VexParameterDef, holder: AnnotationHolder) {
        val identifier = element.identifier
        val paramName = identifier.text

        val functionDef = PsiTreeUtil.getParentOfType(element, VexFunctionDef::class.java) ?: return
        val block = functionDef.block ?: return

        val usages = PsiTreeUtil.findChildrenOfType(block, VexPrimaryExpr::class.java)
        val isUsed = usages.any { expr ->
            expr.identifier?.text == paramName && VexVariableResolver.resolveVariable(expr, paramName) == element
        }

        if (!isUsed) {
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Unused parameter '$paramName'")
                .range(identifier.textRange)
                .textAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
                .create()
        }
    }
}
