package com.github.unclepomedev.houdinivexassist.lang

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
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
            createTextAttributesKey("VEX_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        val OPERATION_SIGN =
            createTextAttributesKey("VEX_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val PARENTHESES = createTextAttributesKey("VEX_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
        val BRACES = createTextAttributesKey("VEX_BRACES", DefaultLanguageHighlighterColors.BRACES)
        val BRACKETS = createTextAttributesKey("VEX_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        val COMMA = createTextAttributesKey("VEX_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val SEMICOLON = createTextAttributesKey("VEX_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
        val DOT = createTextAttributesKey("VEX_DOT", DefaultLanguageHighlighterColors.DOT)

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

        private val OPERATION_SIGN_KEYS = arrayOf(OPERATION_SIGN)
        private val PARENTHESES_KEYS = arrayOf(PARENTHESES)
        private val BRACES_KEYS = arrayOf(BRACES)
        private val BRACKETS_KEYS = arrayOf(BRACKETS)
        private val COMMA_KEYS = arrayOf(COMMA)
        private val SEMICOLON_KEYS = arrayOf(SEMICOLON)
        private val DOT_KEYS = arrayOf(DOT)

        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer {
        return VexLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            VexTypes.IF, VexTypes.ELSE, VexTypes.FOR, VexTypes.FOREACH,
            VexTypes.WHILE, VexTypes.DO, VexTypes.BREAK, VexTypes.CONTINUE,
            VexTypes.RETURN, VexTypes.STRUCT, VexTypes.EXPORT, VexTypes.FUNCTION -> KEYWORD_KEYS

            VexTypes.TYPE -> TYPE_KEYS
            VexTypes.NUMBER -> NUMBER_KEYS
            VexTypes.STRING -> STRING_KEYS
            VexTypes.COMMENT -> COMMENT_KEYS
            VexTypes.ATTRIBUTE -> ATTRIBUTE_KEYS
            VexTypes.MACRO -> MACRO_KEYS
            VexTypes.IDENTIFIER -> IDENTIFIER_KEYS

            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS

            VexTypes.LPAREN, VexTypes.RPAREN -> PARENTHESES_KEYS
            VexTypes.LBRACE, VexTypes.RBRACE -> BRACES_KEYS
            VexTypes.LBRACK, VexTypes.RBRACK -> BRACKETS_KEYS
            VexTypes.COMMA -> COMMA_KEYS
            VexTypes.SEMICOLON -> SEMICOLON_KEYS
            VexTypes.DOT -> DOT_KEYS

            VexTypes.PLUS, VexTypes.MINUS, VexTypes.MUL, VexTypes.DIV, VexTypes.MOD,
            VexTypes.EQUALS, VexTypes.PLUSEQ, VexTypes.MINUSEQ, VexTypes.MULEQ, VexTypes.DIVEQ,
            VexTypes.MODEQ, VexTypes.ANDEQ, VexTypes.OREQ, VexTypes.XOREQ, VexTypes.LSHIFTEQ, VexTypes.RSHIFTEQ,
            VexTypes.EQEQ, VexTypes.NEQ, VexTypes.LT, VexTypes.GT, VexTypes.LE, VexTypes.GE,
            VexTypes.ANDAND, VexTypes.OROR, VexTypes.NOT, VexTypes.PLUSPLUS, VexTypes.MINUSMINUS,
            VexTypes.LSHIFT, VexTypes.RSHIFT, VexTypes.BITAND, VexTypes.BITOR, VexTypes.BITXOR, VexTypes.BITNOT,
            VexTypes.QMARK, VexTypes.COLON, VexTypes.ARROW -> OPERATION_SIGN_KEYS

            else -> EMPTY_KEYS
        }
    }
}
