package com.github.unclepomedev.houdinivexassist.lang

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class VexSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        // attach attribute keys ==========================================================
        val KEYWORD = createTextAttributesKey("VEX_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val NUMBER = createTextAttributesKey("VEX_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val STRING = createTextAttributesKey("VEX_STRING", DefaultLanguageHighlighterColors.STRING)
        val COMMENT = createTextAttributesKey("VEX_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val ATTRIBUTE = createTextAttributesKey("VEX_ATTRIBUTE", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

        val MACRO = createTextAttributesKey("VEX_MACRO", DefaultLanguageHighlighterColors.METADATA)

        val IDENTIFIER = createTextAttributesKey("VEX_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val TYPE = createTextAttributesKey("VEX_TYPE", DefaultLanguageHighlighterColors.KEYWORD)
        val BAD_CHARACTER =
            createTextAttributesKey("VEX_BAD_CHARACTER", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)

        // array per color ================================================================
        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val TYPE_KEYS = arrayOf(TYPE)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val STRING_KEYS = arrayOf(STRING)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val ATTRIBUTE_KEYS = arrayOf(ATTRIBUTE)
        private val MACRO_KEYS = arrayOf(MACRO)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer {
        return VexLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            VexTypes.IF, VexTypes.ELSE, VexTypes.FOR, VexTypes.FOREACH,
            VexTypes.WHILE, VexTypes.DO, VexTypes.BREAK, VexTypes.CONTINUE,
            VexTypes.RETURN, VexTypes.STRUCT -> KEYWORD_KEYS

            VexTypes.TYPE -> TYPE_KEYS
            VexTypes.NUMBER -> NUMBER_KEYS
            VexTypes.STRING -> STRING_KEYS
            VexTypes.COMMENT -> COMMENT_KEYS
            VexTypes.ATTRIBUTE -> ATTRIBUTE_KEYS
            VexTypes.MACRO -> MACRO_KEYS
            VexTypes.IDENTIFIER -> IDENTIFIER_KEYS

            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS

            else -> EMPTY_KEYS
        }
    }
}
