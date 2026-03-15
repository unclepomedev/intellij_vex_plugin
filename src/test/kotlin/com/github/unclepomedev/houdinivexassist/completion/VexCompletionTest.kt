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
        val lookupStrings = lookups!!.map { it.lookupString }
        assertTrue("Completion should contain 'distance'", lookupStrings.contains("distance"))
    }

    fun testLocalVariableCompletion() {
        val code = """
            void main() {
                int my_local_var = 123;
                my_<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            void main() {
                int my_local_var = 123;
                my_local_var<caret>
            }
        """.trimIndent()
        )
    }

    fun testParameterCompletion() {
        val code = """
            void main(float my_param_val) {
                my_<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            void main(float my_param_val) {
                my_param_val<caret>
            }
        """.trimIndent()
        )
    }

    fun testFutureVariableNotCompleted() {
        val code = """
            void main() {
                my_<caret>
                int my_future_var = 1;
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            void main() {
                my_<caret>
                int my_future_var = 1;
            }
        """.trimIndent()
        )
    }
}
