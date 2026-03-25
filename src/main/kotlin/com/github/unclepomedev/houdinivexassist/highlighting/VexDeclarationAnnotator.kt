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

    // --- Syntax Check (correcting destruction caused by hacks) ---

    private fun checkMissingSemicolon(element: VexExprStatement, holder: AnnotationHolder) {
        val hasSemicolon = element.node.findChildByType(VexTypes.SEMICOLON) != null
        if (!hasSemicolon) {
            reportError(holder, element, "Missing ';'")
        }
    }

    // --- Declaration Conflict Checks ---

    private fun checkVariableDeclaration(element: VexDeclarationItem, holder: AnnotationHolder) {
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
        val identifier = element.identifier ?: return
        val structName = identifier.text

        if (isAlreadyDefinedStruct(element, structName)) {
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

    // --- Conflict Checks ---

    private fun isAlreadyDefinedInScope(element: VexDeclarationItem, name: String, scope: PsiElement): Boolean {
        return VexScopeAnalyzer.getDeclarationsInScope(scope).any {
            it != element && it.identifier.text == name && it.textOffset < element.textOffset
        }
    }

    private fun isDefinedAsParameter(name: String, scope: PsiElement): Boolean {
        return VexScopeAnalyzer.getParametersForScope(scope).any { it.identifier.text == name }
    }

    private fun isStandardFunction(name: String, context: PsiElement): Boolean {
        val apiProvider = context.project.getService(VexApiProvider::class.java)
        return apiProvider?.hasFunction(name) == true
    }

    private fun isLocalFunctionBefore(element: PsiElement, name: String): Boolean {
        return VexScopeAnalyzer.getVisibleFunctions(element).any {
            it != element && it.identifier.text == name && it.textOffset < element.textOffset
        }
    }

    private fun isStructNameBefore(element: PsiElement, name: String): Boolean {
        return VexScopeAnalyzer.getVisibleStructs(element).any {
            it != element && it.identifier?.text == name && it.textOffset < element.textOffset
        }
    }

    private fun isAlreadyDefinedStruct(element: VexStructDef, name: String): Boolean {
        return VexScopeAnalyzer.getVisibleStructs(element).any {
            it != element && it.identifier?.text == name && it.textOffset < element.textOffset
        }
    }

    private fun hasExactOverloadConflict(element: VexFunctionDef, name: String): Boolean {
        val myParamTypes = extractParameterTypes(element)
        return VexScopeAnalyzer.getVisibleFunctions(element).any { sibling ->
            sibling != element &&
                    sibling.identifier.text == name &&
                    sibling.textOffset < element.textOffset &&
                    extractParameterTypes(sibling) == myParamTypes
        }
    }

    private fun extractParameterTypes(funcDef: VexFunctionDef) =
        funcDef.parameterListDef?.parameterDefList?.map(VexTypeExtractor::extractType) ?: emptyList()

    // --- Error Reporting Utility ---

    private fun reportError(holder: AnnotationHolder, targetElement: PsiElement, message: String) {
        holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(targetElement.textRange)
            .create()
    }
}
