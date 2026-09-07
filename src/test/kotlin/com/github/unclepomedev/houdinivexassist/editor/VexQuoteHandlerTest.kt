package com.github.unclepomedev.houdinivexassist.editor

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class VexQuoteHandlerTest : BasePlatformTestCase() {

    fun testTypingQuotesInEditorInsertsPair() {
        myFixture.configureByText(VexFileType, "<caret>")
        myFixture.type('"')
        myFixture.checkResult("\"\"")
        assertEquals(1, myFixture.caretOffset)
    }

    fun testTypingClosingQuoteOvertypes() {
        myFixture.configureByText(VexFileType, "\"<caret>\"")
        myFixture.type('"')
        myFixture.checkResult("\"\"")
        assertEquals(2, myFixture.caretOffset)
    }

    fun testBackspaceDeletesQuotePair() {
        myFixture.configureByText(VexFileType, "<caret>")
        myFixture.type('"')
        myFixture.type('\b')
        myFixture.checkResult("")
        assertEquals(0, myFixture.caretOffset)
    }
}
