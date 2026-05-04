package com.github.unclepomedev.houdinivexassist.lexer

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexLexerMacroHandlerTest {

    @Test
    fun testHandleInclude() {
        val match = VexLexerMacroHandler.handleMacro("#include \"test.h\"")
        assertEquals(VexTypes.INCLUDE_KW, match?.type)
        assertEquals(8, match?.keywordLength)
    }

    @Test
    fun testHandleIncludeWithSpaces() {
        val matchWithSpaces = VexLexerMacroHandler.handleMacro("#  include <test.h>")
        assertEquals(VexTypes.INCLUDE_KW, matchWithSpaces?.type)
        assertEquals(10, matchWithSpaces?.keywordLength)
    }

    @Test
    fun testHandleDefine() {
        val match = VexLexerMacroHandler.handleMacro("#define FOO 1")
        assertEquals(VexTypes.DEFINE_KW, match?.type)
        assertEquals(7, match?.keywordLength)
    }

    @Test
    fun testHandleDefineFunc() {
        val matchFunc = VexLexerMacroHandler.handleMacro("#define ADD(a,b) a+b")
        assertEquals(VexTypes.DEFINE_KW, matchFunc?.type)
        assertEquals(7, matchFunc?.keywordLength)
    }

    @Test
    fun testHandleIfdef() {
        val match = VexLexerMacroHandler.handleMacro("#ifdef FOO")
        assertEquals(VexTypes.PP_IFDEF_KW, match?.type)
        assertEquals(6, match?.keywordLength)
    }

    @Test
    fun testHandleIfndef() {
        val match = VexLexerMacroHandler.handleMacro("#ifndef FOO")
        assertEquals(VexTypes.PP_IFNDEF_KW, match?.type)
        assertEquals(7, match?.keywordLength)
    }

    @Test
    fun testHandleUndef() {
        val match = VexLexerMacroHandler.handleMacro("#undef FOO")
        assertEquals(VexTypes.PP_UNDEF_KW, match?.type)
        assertEquals(6, match?.keywordLength)
    }

    @Test
    fun testHandleIf() {
        val match = VexLexerMacroHandler.handleMacro("#if 1")
        assertEquals(VexTypes.PP_IF_KW, match?.type)
        assertEquals(3, match?.keywordLength)
    }

    @Test
    fun testHandleElif() {
        val match = VexLexerMacroHandler.handleMacro("#elif 1")
        assertEquals(VexTypes.PP_ELIF_KW, match?.type)
        assertEquals(5, match?.keywordLength)
    }

    @Test
    fun testHandleElse() {
        val match = VexLexerMacroHandler.handleMacro("#else")
        assertEquals(VexTypes.PP_ELSE_KW, match?.type)
        assertEquals(5, match?.keywordLength)
    }

    @Test
    fun testHandleElseWithComment() {
        val matchWithComment = VexLexerMacroHandler.handleMacro("#else // comment")
        assertEquals(VexTypes.PP_ELSE_KW, matchWithComment?.type)
        assertEquals(16, matchWithComment?.keywordLength)
    }

    @Test
    fun testHandleEndif() {
        val match = VexLexerMacroHandler.handleMacro("#endif")
        assertEquals(VexTypes.PP_ENDIF_KW, match?.type)
        assertEquals(6, match?.keywordLength)
    }

    @Test
    fun testNonMacro() {
        val match = VexLexerMacroHandler.handleMacro("#pragma")
        assertNull(match)
    }

    @Test
    fun testEdgeCases() {
        assertNull(VexLexerMacroHandler.handleMacro("#define"))
        assertNull(VexLexerMacroHandler.handleMacro("#ifdef"))
    }
}
