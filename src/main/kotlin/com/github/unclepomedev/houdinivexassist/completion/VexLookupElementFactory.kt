package com.github.unclepomedev.houdinivexassist.completion

import com.github.unclepomedev.houdinivexassist.services.VexFunction
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons

object VexLookupElementFactory {

    fun createKeyword(name: String): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withBoldness(true)
            .withIcon(AllIcons.Nodes.Type)
            .withInsertHandler { context, _ ->
                val document = context.document
                val offset = context.tailOffset

                val hasNextChar = offset < document.textLength
                val nextChar = if (hasNextChar) document.charsSequence[offset] else null

                if (nextChar == null || nextChar !in " ()[]") {
                    document.insertString(offset, " ")
                    context.editor.caretModel.moveToOffset(offset + 1)
                } else {
                    context.editor.caretModel.moveToOffset(offset)
                }
            }
    }

    fun createVariable(name: String, isParameter: Boolean = false): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withIcon(if (isParameter) AllIcons.Nodes.Parameter else AllIcons.Nodes.Variable)
            .withTypeText(if (isParameter) "parameter" else "local variable")
    }

    fun createLocalFunction(name: String, hasArgs: Boolean): LookupElementBuilder {
        val tailText = if (hasArgs) "(...)" else "()"
        return LookupElementBuilder.create(name)
            .withTypeText("local")
            .withTailText(tailText, true)
            .withIcon(AllIcons.Nodes.Method)
            .withInsertHandler(FunctionInsertHandler(hasArgs))
    }

    fun createStandardFunction(func: VexFunction): LookupElementBuilder {
        return LookupElementBuilder.create(func.name)
            .withTypeText(func.returnType)
            .withTailText("(${func.args.joinToString(", ")})", true)
            .withIcon(AllIcons.Nodes.Function)
            .withInsertHandler(FunctionInsertHandler(func.args.isNotEmpty()))
    }

    fun createStructMember(name: String, typeString: String): LookupElementBuilder {
        return LookupElementBuilder.create(name)
            .withIcon(AllIcons.Nodes.Field)
            .withTypeText(typeString)
    }

    fun createSwizzle(swizzle: String, typeText: String): LookupElementBuilder {
        return LookupElementBuilder.create(swizzle)
            .withIcon(AllIcons.Nodes.Property)
            .withTypeText(typeText)
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
        val hasClosingParen =
            hasParen && offset + 1 < document.textLength && document.charsSequence[offset + 1] == ')'

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
