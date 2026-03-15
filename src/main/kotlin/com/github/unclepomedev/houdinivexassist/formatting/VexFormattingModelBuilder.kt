package com.github.unclepomedev.houdinivexassist.formatting

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings

class VexFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val spacingBuilder = createSpacingBuilder(settings)

        val block = VexBlock(formattingContext.node, null, null, spacingBuilder)

        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            block,
            settings
        )
    }

    private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(settings, VexLanguage.INSTANCE)
            // symbols
            .before(VexTypes.SEMICOLON).spaceIf(false) // no space before `;`
            .before(VexTypes.COMMA).spaceIf(false)     // no space before `,`
            .after(VexTypes.COMMA).spaceIf(true)       // space after `,`

            // parentheses
            .between(VexTypes.RPAREN, VexTypes.BLOCK).spaceIf(true)     // space between `)` and `{`
            .between(VexTypes.IDENTIFIER, VexTypes.LPAREN).spaceIf(false)  // no space between function-name and `(`
            .after(VexTypes.LPAREN).spaceIf(false)
            .before(VexTypes.RPAREN).spaceIf(false)    // e.g. (int a)

            // 1-space before identifier
            .between(VexTypes.TYPE, VexTypes.IDENTIFIER).spaces(1)

            // comparison/logic operators
            .aroundInside(VexTypes.LT, VexTypes.RELATIONAL_EXPR).spaceIf(true)
            .aroundInside(VexTypes.GT, VexTypes.RELATIONAL_EXPR).spaceIf(true)
            .aroundInside(VexTypes.LE, VexTypes.RELATIONAL_EXPR).spaceIf(true)
            .aroundInside(VexTypes.GE, VexTypes.RELATIONAL_EXPR).spaceIf(true)
            .aroundInside(VexTypes.EQEQ, VexTypes.EQUALITY_EXPR).spaceIf(true)
            .aroundInside(VexTypes.NEQ, VexTypes.EQUALITY_EXPR).spaceIf(true)
            .aroundInside(VexTypes.ANDAND, VexTypes.LOGICAL_AND_EXPR).spaceIf(true)
            .aroundInside(VexTypes.OROR, VexTypes.LOGICAL_OR_EXPR).spaceIf(true)

            // arithmetic operators
            .aroundInside(VexTypes.PLUS, VexTypes.ADD_EXPR).spaceIf(true)
            .aroundInside(VexTypes.MINUS, VexTypes.ADD_EXPR).spaceIf(true)
            .aroundInside(VexTypes.MUL, VexTypes.MUL_EXPR).spaceIf(true)
            .aroundInside(VexTypes.DIV, VexTypes.MUL_EXPR).spaceIf(true)
            .aroundInside(VexTypes.MOD, VexTypes.MUL_EXPR).spaceIf(true)
            .aroundInside(VexTypes.LSHIFT, VexTypes.SHIFT_EXPR).spaceIf(true)
            .aroundInside(VexTypes.RSHIFT, VexTypes.SHIFT_EXPR).spaceIf(true)

            // assignment operators
            .around(VexTypes.EQUALS).spaceIf(true)
            .around(VexTypes.PLUSEQ).spaceIf(true)
            .around(VexTypes.MINUSEQ).spaceIf(true)
            .around(VexTypes.MULEQ).spaceIf(true)
            .around(VexTypes.DIVEQ).spaceIf(true)
            .around(VexTypes.MODEQ).spaceIf(true)
            .around(VexTypes.ANDEQ).spaceIf(true)
            .around(VexTypes.OREQ).spaceIf(true)
            .around(VexTypes.XOREQ).spaceIf(true)
            .around(VexTypes.LSHIFTEQ).spaceIf(true)
            .around(VexTypes.RSHIFTEQ).spaceIf(true)

            // bit operators
            .aroundInside(VexTypes.BITOR, VexTypes.BITWISE_OR_EXPR).spaceIf(true)
            .aroundInside(VexTypes.BITXOR, VexTypes.BITWISE_XOR_EXPR).spaceIf(true)
            .aroundInside(VexTypes.BITAND, VexTypes.BITWISE_AND_EXPR).spaceIf(true)

            // ternary operator
            .aroundInside(VexTypes.QMARK, VexTypes.TERNARY_EXPR).spaceIf(true)
            .aroundInside(VexTypes.COLON, VexTypes.TERNARY_EXPR).spaceIf(true)
    }
}
