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
}
