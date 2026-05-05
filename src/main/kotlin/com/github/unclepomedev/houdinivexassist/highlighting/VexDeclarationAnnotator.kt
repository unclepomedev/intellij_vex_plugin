package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.github.unclepomedev.houdinivexassist.types.VexTypeExtractor
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class VexDeclarationAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is VexDeclarationItem -> checkVariableDeclaration(element, holder)
            is VexFunctionDef -> checkFunctionDefinition(element, holder)
            is VexStructDef -> checkStructDefinition(element, holder)
            is VexExprStatement -> checkMissingSemicolon(element, holder)
        }
    }

    private fun checkMissingSemicolon(element: VexExprStatement, holder: AnnotationHolder) {
        if (element.node.findChildByType(VexTypes.SEMICOLON) == null) {
            reportError(holder, element, "Missing ';'")
        }
    }

    private fun checkVariableDeclaration(element: VexDeclarationItem, holder: AnnotationHolder) {
        if (!VexPreprocessorEvaluator.isActive(element)) return
        val identifier = element.identifier
        val varName = identifier.text
        val scope = VexScopeAnalyzer.findDeclarationScope(element) ?: return

        if (isAlreadyDefinedInScope(element, varName, scope)) {
            reportError(holder, identifier, "Variable '$varName' is already defined in this scope")
            return
        }
        if (isDefinedAsParameter(varName, scope)) {
            reportError(holder, identifier, "Variable '$varName' is already defined as a parameter")
            return
        }
        if (isLocalFunctionBefore(element, varName)) {
            reportError(holder, identifier, "Variable name '$varName' conflicts with a local function")
            return
        }
    }

    private fun checkFunctionDefinition(element: VexFunctionDef, holder: AnnotationHolder) {
        if (!VexPreprocessorEvaluator.isActive(element)) return
        val identifier = element.identifier
        val funcName = identifier.text

        if (isStandardFunction(funcName, element)) {
            reportError(holder, identifier, "Function name '$funcName' conflicts with a standard VEX function")
            return
        }
        if (isStructNameBefore(element, funcName)) {
            reportError(holder, identifier, "Function name '$funcName' conflicts with a struct definition")
            return
        }
        if (hasExactOverloadConflict(element, funcName)) {
            val paramCount = element.parameterListDef?.parameterDefList?.size ?: 0
            reportError(holder, identifier, "Function '$funcName' with $paramCount parameters is already defined")
            return
        }
    }

    private fun checkStructDefinition(element: VexStructDef, holder: AnnotationHolder) {
        if (!VexPreprocessorEvaluator.isActive(element)) return
        val identifier = element.identifier ?: return
        val structName = identifier.text

        if (isStructNameBefore(element, structName)) {
            reportError(holder, identifier, "Struct '$structName' is already defined")
            return
        }
        if (isStandardFunction(structName, element)) {
            reportError(holder, identifier, "Struct name '$structName' conflicts with a standard VEX function")
            return
        }
        if (isLocalFunctionBefore(element, structName)) {
            reportError(holder, identifier, "Struct name '$structName' conflicts with a local function")
            return
        }
    }

    private fun isAlreadyDefinedInScope(element: VexDeclarationItem, name: String, scope: PsiElement): Boolean {
        return VexScopeAnalyzer.getDeclarationsInScope(scope).any { prior ->
            val isPrior = prior.containingFile != element.containingFile || prior.textOffset < element.textOffset
            prior != element && prior.identifier.text == name && isPrior
        }
    }

    private fun isDefinedAsParameter(name: String, scope: PsiElement): Boolean {
        return VexScopeAnalyzer.getParametersForScope(scope).any { it.identifier.text == name }
    }

    private fun isStandardFunction(name: String, context: PsiElement): Boolean {
        return context.project.getService(VexApiProvider::class.java)?.hasFunction(name) == true
    }

    private fun isLocalFunctionBefore(element: PsiElement, name: String): Boolean {
        val candidates = VexScopeAnalyzer.getVisibleFunctionsGrouped(element)[name] ?: return false
        return candidates.any { it.textOffset < element.textOffset }
    }

    private fun isStructNameBefore(element: PsiElement, name: String): Boolean {
        val candidates = VexScopeAnalyzer.getVisibleStructsGrouped(element)[name] ?: return false
        return candidates.any { it.textOffset < element.textOffset }
    }

    private fun hasExactOverloadConflict(element: VexFunctionDef, name: String): Boolean {
        val candidates = VexScopeAnalyzer.getVisibleFunctionsGrouped(element)[name] ?: return false
        val myParamTypes = extractParameterTypes(element)

        return candidates.any { sibling ->
            sibling != element &&
                    sibling.textOffset < element.textOffset &&
                    extractParameterTypes(sibling) == myParamTypes
        }
    }

    private fun extractParameterTypes(funcDef: VexFunctionDef) =
        funcDef.parameterListDef?.parameterDefList?.map(VexTypeExtractor::extractType) ?: emptyList()

    private fun reportError(holder: AnnotationHolder, targetElement: PsiElement, message: String) {
        holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(targetElement.textRange)
            .create()
    }
}
