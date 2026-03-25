package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.psi.*
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
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
        val isStructField = structDef != null
        val parentStructName = structDef?.identifier?.text

        val isUsed = if (isStructField && parentStructName != null) {
            val project = element.project
            val virtualFiles = FileTypeIndex.getFiles(VexFileType, GlobalSearchScope.projectScope(project))
            val files = virtualFiles.mapNotNull { PsiManager.getInstance(project).findFile(it) as? VexFile }

            files.any { file ->
                val relevantAccesses = VexUsageAnalyzer.getMemberAccesses(file, varName)
                relevantAccesses.any { access ->
                    val baseType = VexTypeInference.inferType(access.expr)
                    baseType is VexType.StructType && baseType.name == parentStructName
                }
            }
        } else {
            val scope = VexScopeAnalyzer.findDeclarationScope(element) ?: return
            if (scope is VexFile) {
                val project = element.project
                val virtualFiles = FileTypeIndex.getFiles(VexFileType, GlobalSearchScope.projectScope(project))
                val files = virtualFiles.mapNotNull { PsiManager.getInstance(project).findFile(it) as? VexFile }

                files.any { file ->
                    val usages = VexUsageAnalyzer.getVariableUsages(file, varName)
                    usages.any { expr ->
                        VexVariableResolver.resolveVariable(expr, varName) == element
                    }
                }
            } else {
                val usages = VexUsageAnalyzer.getVariableUsages(scope, varName)
                usages.any { expr ->
                    VexVariableResolver.resolveVariable(expr, varName) == element
                }
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

        // entrypoint is not marked as unused
        val fileBaseName = file.virtualFile?.nameWithoutExtension
        val sanitizedBaseName = fileBaseName?.replace(Regex("[^A-Za-z0-9_]"), "_")
        if (funcName == "main" || (sanitizedBaseName != null && funcName == sanitizedBaseName)) return

        val project = element.project
        val virtualFiles = FileTypeIndex.getFiles(VexFileType, GlobalSearchScope.projectScope(project))
        val files = virtualFiles.mapNotNull { PsiManager.getInstance(project).findFile(it) as? VexFile }

        val isUsed = files.any { f ->
            val usages = VexUsageAnalyzer.getFunctionCalls(f, funcName)
            usages.any { call ->
                val argTypes = call.argumentList?.exprList?.map(VexTypeInference::inferType) ?: emptyList()
                val resolved = VexFunctionResolver.resolveFunction(
                    element = call,
                    functionName = funcName,
                    argTypes = argTypes
                ) ?: VexFunctionResolver.resolveFunction(
                    element = call,
                    functionName = funcName,
                    arity = argTypes.size
                )
                resolved == element
            }
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

        val usages = VexUsageAnalyzer.getVariableUsages(block, paramName)
        val isUsed = usages.any { expr ->
            VexVariableResolver.resolveVariable(expr, paramName) == element
        }

        if (!isUsed) {
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Unused parameter '$paramName'")
                .range(identifier.textRange)
                .textAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
                .create()
        }
    }
}
