package com.github.unclepomedev.houdinivexassist.editor

import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase

@Suppress("UnstableApiUsage")
class VexInlayHintsProviderTest : InlayHintsProviderTestCase() {

    // Workaround for an issue where test framework markers (`<# ... #>`) break VEX parsing and no hints are generated.
    private fun doTest(text: String) {
        val expectedHints = mutableMapOf<Int, String>()
        var strippedText = text
        val regex = Regex("<#\\s*([^>]+?)\\s*#>")
        var matcher = regex.find(strippedText)
        while (matcher != null) {
            val offset = matcher.range.first
            val hintText = matcher.groupValues[1].trim()
            expectedHints[offset] = hintText
            strippedText = strippedText.removeRange(matcher.range)
            matcher = regex.find(strippedText)
        }

        myFixture.configureByText("test.vfl", strippedText)
        val file = myFixture.file
        val editor = myFixture.editor

        val provider = VexInlayHintsProvider()
        val settings = provider.createSettings()

        val actualHints = mutableMapOf<Int, String>()
        val sink = object : com.intellij.codeInsight.hints.InlayHintsSink {
            override fun addBlockElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                showAbove: Boolean,
                priority: Int,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation
            ) {
            }

            override fun addInlineElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation,
                placeAtTheEndOfLine: Boolean
            ) {
                actualHints[offset] = presentation.toString()
            }

            // For newer API compatibility
            override fun addBlockElement(
                logicalLine: Int,
                showAbove: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.BlockConstraints?
            ) {
            }

            override fun addInlineElement(
                offset: Int,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.HorizontalConstraints?
            ) {
                actualHints[offset] = presentation.toString()
            }
        }

        val collector = provider.getCollectorFor(file, editor, settings, sink)
        if (collector != null) {
            file.accept(object : com.intellij.psi.PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: com.intellij.psi.PsiElement) {
                    collector.collect(element, editor, sink)
                    super.visitElement(element)
                }
            })
        }

        assertEquals("Number of hints", expectedHints.size, actualHints.size)
        for ((offset, expectedText) in expectedHints) {
            val actualText = actualHints[offset]
            assertNotNull(
                "Expected hint '$expectedText' at offset $offset but found none. Actual hints: $actualHints",
                actualText
            )
            assertTrue("Expected hint '$expectedText' but was '$actualText'", actualText!!.contains(expectedText))
        }
    }

    fun testInlayHints() {
        val text = """
            void myFunc(int a, float b) {}
            
            void main() {
                myFunc(<# a: #>1, <# b: #>2.0);
            }
        """.trimIndent()
        doTest(text)
    }

    fun testInlayHintsOverloads() {
        val text = """
            void myFunc(int x) {}
            void myFunc(float y) {}
            
            void main() {
                myFunc(<# x: #>1);
                myFunc(<# y: #>2.0);
            }
        """.trimIndent()
        doTest(text)
    }

    fun testStandardFunctionInlayHints() {
        val text = """
            void main() {
                pow(<# n: #>2.0, <# exponent: #>3.0);
            }
        """.trimIndent()
        doTest(text)
    }

    fun testNoHintsForUnknownFunction() {
        // expectedHints 0, pass if actualHints 0
        val text = "void main() { unknownFunc(1, 2); }"
        doTest(text)
    }

    fun testNoHintsForEmptyArgs() {
        val text = "void main() { getbbox_center(); }"
        doTest(text)
    }

    fun testVariadicFunction() {
        // printf(string format, ...) only gives a hint for the first element.
        val text = """
            void main() {
                printf(<# format: #>"%d", 123);
            }
        """.trimIndent()
        doTest(text)
    }
}
