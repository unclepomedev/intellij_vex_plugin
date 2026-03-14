package com.github.unclepomedev.houdinivexassist

import com.github.unclepomedev.houdinivexassist.lang.VexLexerAdapter
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
        doTest("int a;",
            VexTypes.TYPE to "int",
            TokenType.WHITE_SPACE to " ",
            VexTypes.IDENTIFIER to "a",
            VexTypes.SEMICOLON to ";"
        )

        doTest("internal",
            VexTypes.IDENTIFIER to "internal"
        )
    }

    fun testAttributes() {
        doTest("@P v@Cd i@ptnum s@name",
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
        doTest("// comment\nint",
            VexTypes.COMMENT to "// comment",
            TokenType.WHITE_SPACE to "\n",
            VexTypes.TYPE to "int"
        )
        doTest("/* comment\n */int",
            VexTypes.COMMENT to "/* comment\n */",
            VexTypes.TYPE to "int"
        )
    }

    fun testStrings() {
        doTest("\"double\"",
            VexTypes.STRING to "\"double\""
        )
        doTest("'single'",
            VexTypes.STRING to "'single'"
        )
    }

    fun testUnknownCharacters() {
        doTest("　あ\u0000",
            TokenType.BAD_CHARACTER to "　",
            TokenType.BAD_CHARACTER to "あ",
            TokenType.BAD_CHARACTER to "\u0000"
        )
    }
}
