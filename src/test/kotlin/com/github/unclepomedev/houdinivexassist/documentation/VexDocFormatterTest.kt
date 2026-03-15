package com.github.unclepomedev.houdinivexassist.documentation

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class VexDocFormatterTest {

    @Test
    fun testFormat_RemovesMetadata() {
        val rawText = """
            = abs =
            #type: vex
            #context: all
            
            This is content.
        """.trimIndent()

        val html = VexDocFormatter.format("abs", rawText)

        assertFalse("Should remove '=' titles", html.contains("= abs ="))
        assertFalse("Should remove '#' metadata", html.contains("#type: vex"))
        assertTrue("Should keep content", html.contains("This is content."))
        assertTrue("Should show function name when no usages", html.contains("<b>abs</b>"))
    }

    @Test
    fun testFormat_ExtractsUsages() {
        val rawText = """
            :usage: `int abs(int n)`
            :usage: `float abs(float n)`
            
            Content.
        """.trimIndent()

        val html = VexDocFormatter.format("abs", rawText)

        assertTrue(html.contains("int abs(int n)<br>float abs(float n)"))
        assertFalse(html.contains(":usage:"))
    }

    @Test
    fun testFormat_FormatsCodeBlocks() {
        val rawText = """
            :box:Scalar example
                {{{
                #!vex
                if (abs(n) > 1) {
                    // n is greater than 1
                }
                }}}
        """.trimIndent()

        val html = VexDocFormatter.format("abs", rawText)

        assertTrue("Should contain <pre><code>", html.contains("<pre><code>"))
        assertTrue("Should preserve code block structure", html.contains("if (abs(n) &gt; 1) {"))
    }

    @Test
    fun testFormat_FormatsCodeBlocksUnindented() {
        val rawText = """
            {{{
            #!vex
            if (abs(n) > 1) {
            }
            }}}
        """.trimIndent()

        val html = VexDocFormatter.format("abs", rawText)

        assertTrue("Should contain <pre><code>", html.contains("<pre><code>"))
        assertTrue("Should preserve code block structure", html.contains("if (abs(n) &gt; 1) {"))
    }

    @Test
    fun testFormat_HandlesUnclosedCodeBlock() {
        val rawText = """
        {{{
        #!vex
        int x = 1;
    """.trimIndent()

        val html = VexDocFormatter.format("test", rawText)

        assertTrue("Should handle unclosed block", html.isNotEmpty())
    }

    @Test
    fun testFormat_HandlesNoCodeBlocks() {
        val rawText = """
        :usage: `int foo()`
        
        Simple description with no code blocks.
    """.trimIndent()

        val html = VexDocFormatter.format("foo", rawText)

        assertTrue("Should contain description", html.contains("Simple description"))
        assertFalse("Should not contain code tags", html.contains("<pre><code>"))
    }
}
