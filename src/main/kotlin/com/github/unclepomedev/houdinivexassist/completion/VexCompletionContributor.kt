package com.github.unclepomedev.houdinivexassist.completion

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionDef
import com.github.unclepomedev.houdinivexassist.psi.VexScopeAnalyzer
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class VexCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(VexTypes.IDENTIFIER).withLanguage(VexLanguage.INSTANCE),
            VexCompletionProvider()
        )
    }
}

private class VexCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val localFunctionNames = addLocalFunctions(parameters, result)
        addStandardFunctions(parameters, result, localFunctionNames)
        addLocalVariablesAndParameters(parameters, result)
    }

    private fun addLocalVariablesAndParameters(
        parameters: CompletionParameters,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val currentOffset = element.textOffset
        var currentScope = VexScopeAnalyzer.findDeclarationScope(element)

        // Traverse scopes upwards and delegate the extraction
        while (currentScope != null) {
            addVariablesFromScope(currentScope, currentOffset, result)
            addParametersFromScope(currentScope, result)
            currentScope = VexScopeAnalyzer.findDeclarationScope(currentScope.parent)
        }
    }

    private fun addVariablesFromScope(
        scope: PsiElement,
        currentOffset: Int,
        result: CompletionResultSet
    ) {
        VexScopeAnalyzer.getDeclarationsInScope(scope)
            .filter { it.textOffset < currentOffset }
            .map { it.identifier.text }
            .filter { it.isNotEmpty() }
            .forEach { name ->
                result.addElement(createVariableLookup(name))
            }
    }

    private fun addParametersFromScope(
        scope: PsiElement,
        result: CompletionResultSet
    ) {
        VexScopeAnalyzer.getParametersForScope(scope)
            .map { it.identifier.text }
            .filter { it.isNotEmpty() }
            .forEach { name ->
                result.addElement(createParameterLookup(name))
            }
    }

    private fun createVariableLookup(name: String): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withIcon(AllIcons.Nodes.Variable)
            .withTypeText("local variable")
    }

    private fun createParameterLookup(name: String): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withIcon(AllIcons.Nodes.Parameter)
            .withTypeText("parameter")
    }

    private fun addLocalFunctions(
        parameters: CompletionParameters,
        result: CompletionResultSet
    ): Set<String> {
        val containingFile = parameters.originalFile
        val localFunctions = PsiTreeUtil.findChildrenOfType(containingFile, VexFunctionDef::class.java)
        val addedNames = mutableSetOf<String>()

        localFunctions.forEach { localFunc ->
            val funcName = localFunc.identifier.text ?: return@forEach
            val hasArgs = localFunc.parameterListDef?.parameterDefList?.isNotEmpty() == true
            val tailText = if (hasArgs) "(...)" else "()"

            val lookupElement = LookupElementBuilder.create(funcName)
                .withTypeText("local")
                .withTailText(tailText, true)
                .withIcon(AllIcons.Nodes.Method)
                .withInsertHandler(FunctionInsertHandler(hasArgs = hasArgs))

            result.addElement(lookupElement)
            addedNames.add(funcName)
        }
        return addedNames
    }

    private fun addStandardFunctions(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        skipNames: Set<String>
    ) {
        val vexApiProvider = parameters.originalFile.project.getService(VexApiProvider::class.java) ?: return

        vexApiProvider.functions.forEach { func ->
            if (func.name in skipNames) return@forEach

            val lookupElement = LookupElementBuilder.create(func.name)
                .withTypeText(func.returnType)
                .withTailText("(${func.args.joinToString(", ")})", true)
                .withIcon(AllIcons.Nodes.Function)
                .withInsertHandler(FunctionInsertHandler(hasArgs = func.args.isNotEmpty()))

            result.addElement(lookupElement)
        }
    }
}

/**
 * Handler that inserts `()` and moves the cursor to the appropriate position immediately after completion is confirmed.
 */
private class FunctionInsertHandler(private val hasArgs: Boolean) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = context.document
        val offset = context.tailOffset

        if (context.completionChar == '(') {
            context.setAddCompletionChar(false)
        }

        val hasParen = offset < document.textLength && document.charsSequence[offset] == '('
        val hasClosingParen = hasParen && offset + 1 < document.textLength && document.charsSequence[offset + 1] == ')'

        if (!hasParen) {
            document.insertString(offset, "()")
        }

        // Calculate cursor movement:
        // Move outside (+2) ONLY IF the function has NO arguments AND it has cleanly closed empty parentheses `()`.
        // Otherwise, move inside (+1).
        val moveOffset = if (!hasArgs && (!hasParen || hasClosingParen)) offset + 2 else offset + 1
        editor.caretModel.moveToOffset(moveOffset)
    }
}
