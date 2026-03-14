package com.github.unclepomedev.houdinivexassist

import com.github.unclepomedev.houdinivexassist.lexer.VexLexerAdapter
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class VexLexerTest : VexTestBase() {
    private fun doTest(text: String, vararg expectedTokens: Pair<IElementType, String>) {
        val lexer = VexLexerAdapter()
        lexer.start(text)

        for ((expectedType, expectedText) in expectedTokens) {
            assertEquals("Token type mismatch for text: $expectedText", expectedType, lexer.tokenType)
            assertEquals("Token text mismatch", expectedText, lexer.tokenText)
            lexer.advance()
        }
        assertNull("Lexer should have finished", lexer.tokenType)
    }

    fun testKeywordAndIdentifier() {
        doTest(
            "int a;",
            VexTypes.TYPE to "int",
            TokenType.WHITE_SPACE to " ",
            VexTypes.IDENTIFIER to "a",
            VexTypes.SEMICOLON to ";"
        )

        doTest(
            "internal", VexTypes.IDENTIFIER to "internal"
        )
    }

    fun testAttributes() {
        doTest(
            "@P v@Cd i@ptnum s@name",
            VexTypes.ATTRIBUTE to "@P",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ATTRIBUTE to "v@Cd",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ATTRIBUTE to "i@ptnum",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ATTRIBUTE to "s@name"
        )
    }

    fun testComments() {
        doTest(
            "// comment\nint", VexTypes.COMMENT to "// comment", TokenType.WHITE_SPACE to "\n", VexTypes.TYPE to "int"
        )
        doTest(
            "/* comment\n */int", VexTypes.COMMENT to "/* comment\n */", VexTypes.TYPE to "int"
        )
    }

    fun testStrings() {
        doTest(
            "\"double\"", VexTypes.STRING to "\"double\""
        )
        doTest(
            "'single'", VexTypes.STRING to "'single'"
        )
    }

    fun testUnknownCharacters() {
        doTest(
            "　あ\u0000",
            TokenType.BAD_CHARACTER to "　",
            TokenType.BAD_CHARACTER to "あ",
            TokenType.BAD_CHARACTER to "\u0000"
        )
    }

    fun testOperators() {
        doTest(
            "== != < > <= >= + - * / % && || ! ? : = += -= *= /= %= &= |= ^= <<= >>= << >> & | ^ ~",
            VexTypes.EQEQ to "==",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NEQ to "!=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.LT to "<",
            TokenType.WHITE_SPACE to " ",
            VexTypes.GT to ">",
            TokenType.WHITE_SPACE to " ",
            VexTypes.LE to "<=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.GE to ">=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.PLUS to "+",
            TokenType.WHITE_SPACE to " ",
            VexTypes.MINUS to "-",
            TokenType.WHITE_SPACE to " ",
            VexTypes.MUL to "*",
            TokenType.WHITE_SPACE to " ",
            VexTypes.DIV to "/",
            TokenType.WHITE_SPACE to " ",
            VexTypes.MOD to "%",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ANDAND to "&&",
            TokenType.WHITE_SPACE to " ",
            VexTypes.OROR to "||",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NOT to "!",
            TokenType.WHITE_SPACE to " ",
            VexTypes.QMARK to "?",
            TokenType.WHITE_SPACE to " ",
            VexTypes.COLON to ":",
            TokenType.WHITE_SPACE to " ",
            VexTypes.EQUALS to "=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.PLUSEQ to "+=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.MINUSEQ to "-=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.MULEQ to "*=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.DIVEQ to "/=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.MODEQ to "%=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ANDEQ to "&=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.OREQ to "|=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.XOREQ to "^=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.LSHIFTEQ to "<<=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.RSHIFTEQ to ">>=",
            TokenType.WHITE_SPACE to " ",
            VexTypes.LSHIFT to "<<",
            TokenType.WHITE_SPACE to " ",
            VexTypes.RSHIFT to ">>",
            TokenType.WHITE_SPACE to " ",
            VexTypes.BITAND to "&",
            TokenType.WHITE_SPACE to " ",
            VexTypes.BITOR to "|",
            TokenType.WHITE_SPACE to " ",
            VexTypes.BITXOR to "^",
            TokenType.WHITE_SPACE to " ",
            VexTypes.BITNOT to "~"
        )
    }

    fun testMiscellaneousTokens() {
        doTest(
            "123 { } ( ) [ ] -> , #define",
            VexTypes.NUMBER to "123",
            TokenType.WHITE_SPACE to " ",
            VexTypes.LBRACE to "{",
            TokenType.WHITE_SPACE to " ",
            VexTypes.RBRACE to "}",
            TokenType.WHITE_SPACE to " ",
            VexTypes.LPAREN to "(",
            TokenType.WHITE_SPACE to " ",
            VexTypes.RPAREN to ")",
            TokenType.WHITE_SPACE to " ",
            VexTypes.LBRACK to "[",
            TokenType.WHITE_SPACE to " ",
            VexTypes.RBRACK to "]",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ARROW to "->",
            TokenType.WHITE_SPACE to " ",
            VexTypes.COMMA to ",",
            TokenType.WHITE_SPACE to " ",
            VexTypes.MACRO to "#define"
        )
    }

    fun testControlKeywords() {
        doTest(
            "if else for foreach while do break continue return",
            VexTypes.IF to "if",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ELSE to "else",
            TokenType.WHITE_SPACE to " ",
            VexTypes.FOR to "for",
            TokenType.WHITE_SPACE to " ",
            VexTypes.FOREACH to "foreach",
            TokenType.WHITE_SPACE to " ",
            VexTypes.WHILE to "while",
            TokenType.WHITE_SPACE to " ",
            VexTypes.DO to "do",
            TokenType.WHITE_SPACE to " ",
            VexTypes.BREAK to "break",
            TokenType.WHITE_SPACE to " ",
            VexTypes.CONTINUE to "continue",
            TokenType.WHITE_SPACE to " ",
            VexTypes.RETURN to "return"
        )
    }

    fun testArrayAttributes() {
        doTest(
            "f[]@uvs i[]@pts []@myattr",
            VexTypes.ATTRIBUTE to "f[]@uvs",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ATTRIBUTE to "i[]@pts",
            TokenType.WHITE_SPACE to " ",
            VexTypes.ATTRIBUTE to "[]@myattr"
        )
    }

    fun testTypes() {
        doTest(
            "int float vector vector2 vector4 matrix matrix3 string void bsdf dict",
            VexTypes.TYPE to "int",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "float",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "vector",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "vector2",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "vector4",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "matrix",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "matrix3",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "string",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "void",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "bsdf",
            TokenType.WHITE_SPACE to " ",
            VexTypes.TYPE to "dict"
        )
    }

    fun testFloatNumbers() {
        doTest(
            "42 3.14159 10.",
            VexTypes.NUMBER to "42",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "3.14159",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "10."
        )
    }

    fun testAdvancedKeywords() {
        doTest(
            "struct export function",
            VexTypes.STRUCT to "struct",
            TokenType.WHITE_SPACE to " ",
            VexTypes.EXPORT to "export",
            TokenType.WHITE_SPACE to " ",
            VexTypes.FUNCTION to "function"
        )
    }

    fun testNumbers() {
        doTest(
            "42 3.14159 10. .5 1e-5 2.5E4 0x1A 0Xff",
            VexTypes.NUMBER to "42",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "3.14159",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "10.",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to ".5",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "1e-5",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "2.5E4",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "0x1A",
            TokenType.WHITE_SPACE to " ",
            VexTypes.NUMBER to "0Xff"
        )
    }
}
