package com.github.unclepomedev.houdinivexassist.completion

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType

class VexCompletionTest : VexTestBase() {
    fun testFunctionCompletion() {
        val code = """
            void main() {
                dis<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion list should not be null", lookups)
        val lookupStrings = lookups.map { it.lookupString }
        assertTrue("Completion should contain 'distance'", lookupStrings.contains("distance"))
    }
}