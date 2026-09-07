package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
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
        val structDef = PsiTreeUtil.getParentOfType(element, VexStructDef::class.java)

        val isUsed =
            if (structDef != null) {
                val parentStructName = structDef.identifier?.text ?: return
                isStructFieldUsed(element, varName, parentStructName)
            } else {
                isVariableUsed(element, varName)
            }

        if (!isUsed) {
            val messageType = if (structDef != null) "field" else "variable"
            reportUnused(holder, identifier, "Unused $messageType '$varName'")
        }
    }

    private fun isStructFieldUsed(
        element: VexDeclarationItem,
        fieldName: String,
        structName: String,
    ): Boolean {
        val files = VexUsageAnalyzer.getAllProjectVexFiles(element.project)
        return files.any { file ->
            VexUsageAnalyzer.getMemberAccesses(file, fieldName).any { access ->
                val baseType = VexTypeInference.inferType(access.expr)
                baseType is VexType.StructType && baseType.name == structName
            }
        }
    }

    private fun isVariableUsed(element: VexDeclarationItem, varName: String): Boolean {
        val scope = VexScopeAnalyzer.findDeclarationScope(element) ?: return true
        val targetScopes =
            when (scope) {
                is VexFile -> VexUsageAnalyzer.getAllProjectVexFiles(element.project)
                else -> listOf(scope)
            }

        return targetScopes.any { target ->
            VexUsageAnalyzer.getVariableUsages(target, varName).any { expr ->
                VexVariableResolver.resolveVariable(expr, varName) == element
            }
        }
    }

    private fun checkUnusedFunction(element: VexFunctionDef, holder: AnnotationHolder) {
        val identifier = element.identifier
        val funcName = identifier.text
        val file = element.containingFile as? VexFile ?: return

        if (isEntryPoint(file, funcName)) return

        val isUsed = isFunctionUsed(element, funcName)
        if (!isUsed) {
            reportUnused(holder, identifier, "Unused function '$funcName'")
        }
    }

    private fun isEntryPoint(file: VexFile, funcName: String): Boolean {
        if (funcName == "main") return true
        val fileBaseName = file.virtualFile?.nameWithoutExtension ?: return false
        val sanitizedBaseName = fileBaseName.replace(Regex("[^A-Za-z0-9_]"), "_")
        return funcName == sanitizedBaseName
    }

    private fun isFunctionUsed(element: VexFunctionDef, funcName: String): Boolean {
        val files = VexUsageAnalyzer.getAllProjectVexFiles(element.project)
        return files.any { file ->
            VexUsageAnalyzer.getFunctionCalls(file, funcName).any { call ->
                resolvesTo(call, funcName, element)
            }
        }
    }

    private fun resolvesTo(
        call: VexCallExpr,
        funcName: String,
        expectedDef: VexFunctionDef,
    ): Boolean {
        val argTypes = call.argumentList?.exprList?.map(VexTypeInference::inferType).orEmpty()
        val resolved =
            VexFunctionResolver.resolveFunction(
                element = call,
                functionName = funcName,
                argTypes = argTypes,
            )
                ?: VexFunctionResolver.resolveFunction(
                    element = call,
                    functionName = funcName,
                    arity = argTypes.size,
                )
        return resolved == expectedDef
    }

    private fun checkUnusedParameter(element: VexParameterDef, holder: AnnotationHolder) {
        val identifier = element.identifier
        val paramName = identifier.text

        val functionDef = PsiTreeUtil.getParentOfType(element, VexFunctionDef::class.java) ?: return
        val block = functionDef.block ?: return

        val isUsed =
            VexUsageAnalyzer.getVariableUsages(block, paramName).any { expr ->
                VexVariableResolver.resolveVariable(expr, paramName) == element
            }

        if (!isUsed) {
            reportUnused(holder, identifier, "Unused parameter '$paramName'")
        }
    }

    private fun reportUnused(holder: AnnotationHolder, identifier: PsiElement, message: String) {
        holder
            .newAnnotation(HighlightSeverity.WEAK_WARNING, message)
            .range(identifier.textRange)
            .textAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
            .create()
    }
}
