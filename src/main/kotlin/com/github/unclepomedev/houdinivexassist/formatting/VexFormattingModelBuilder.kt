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
            .after(VexTypes.COMMA).spaceIf(true)       // space after `,`

            // operators
            .around(VexTypes.EQUALS).spaceIf(true)     // `=`
            .around(VexTypes.PLUSEQ).spaceIf(true)     // `+=`
            .around(VexTypes.MINUSEQ).spaceIf(true)    // `-=`
            .around(VexTypes.MULEQ).spaceIf(true)      // `*=`
            .around(VexTypes.DIVEQ).spaceIf(true)      // `/=`
            .around(VexTypes.PLUS).spaceIf(true)       // `+`
            .around(VexTypes.MINUS).spaceIf(true)      // `-`
            .around(VexTypes.MUL).spaceIf(true)        // `*`
            .around(VexTypes.DIV).spaceIf(true)        // `/`
            .around(VexTypes.LT).spaceIf(true)         // `<`
            .around(VexTypes.GT).spaceIf(true)         // `>`
            .around(VexTypes.LE).spaceIf(true)         // `<=`
            .around(VexTypes.GE).spaceIf(true)         // `>=`
            .around(VexTypes.EQEQ).spaceIf(true)       // `==`
            .around(VexTypes.NEQ).spaceIf(true)        // `!=`

            // parentheses
            .between(VexTypes.RPAREN, VexTypes.BLOCK).spaceIf(true)     // space between `)` and `{`
            .between(VexTypes.IDENTIFIER, VexTypes.LPAREN).spaceIf(false)  // no space between function-name and `(`
            .after(VexTypes.LPAREN).spaceIf(false)
            .before(VexTypes.RPAREN).spaceIf(false)    // e.g. (int a)

            .between(VexTypes.TYPE, VexTypes.IDENTIFIER).spaces(1)
    }
}
