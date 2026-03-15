package com.github.unclepomedev.houdinivexassist.completion

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionDef
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
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
        addStandardFunctions(parameters, result)
        addLocalFunctions(parameters, result)
    }

    private fun addStandardFunctions(parameters: CompletionParameters, result: CompletionResultSet) {
        val vexApiProvider = parameters.originalFile.project.getService(VexApiProvider::class.java) ?: return

        vexApiProvider.functions.forEach { func ->
            val lookupElement = LookupElementBuilder.create(func.name)
                .withTypeText(func.returnType)
                .withTailText("(${func.args.joinToString(", ")})", true)
                .withIcon(AllIcons.Nodes.Function)
                .withInsertHandler(FunctionInsertHandler(hasArgs = func.args.isNotEmpty()))

            result.addElement(lookupElement)
        }
    }

    private fun addLocalFunctions(parameters: CompletionParameters, result: CompletionResultSet) {
        val containingFile = parameters.originalFile
        val localFunctions = PsiTreeUtil.findChildrenOfType(containingFile, VexFunctionDef::class.java)

        localFunctions.forEach { localFunc ->
            val funcName = localFunc.identifier.text ?: return@forEach

            val lookupElement = LookupElementBuilder.create(funcName)
                .withTypeText("local")
                .withTailText("(...)", true)
                .withIcon(AllIcons.Nodes.Method)
                .withInsertHandler(FunctionInsertHandler(hasArgs = true))

            result.addElement(lookupElement)
        }
    }
}

/**
 * Handler that inserts `()` and moves the cursor to the appropriate position immediately after completion is confirmed.
 */
class FunctionInsertHandler(private val hasArgs: Boolean) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = context.document
        val offset = context.tailOffset

        val hasParen = offset < document.textLength && document.charsSequence[offset] == '('

        if (!hasParen) {
            document.insertString(offset, "()")
        }

        // Calculate cursor movement:
        // If the function already has `(` or has "arguments", go inside the parentheses (+1).
        // If you create new parentheses without arguments, move outside the parentheses (+2).
        val moveOffset = if (hasParen || hasArgs) offset + 1 else offset + 2
        editor.caretModel.moveToOffset(moveOffset)
    }
}
