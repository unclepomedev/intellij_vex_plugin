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
        val lookups = myFixture.completeBasic()
        val lookupStrings = lookups?.map { it.lookupString }.orEmpty()
        assertFalse(
            "Completion should not contain future variable 'my_future_var'",
            lookupStrings.contains("my_future_var")
        )

        myFixture.checkResult(
            """
            void main() {
                my_<caret>
                int my_future_var = 1;
            }
        """.trimIndent()
        )
    }

    fun testVariableShadowingCompletion() {
        val code = """
            int my_shadow_var = 1;
            void main() {
                int my_shadow_var = 2;
                if (1) {
                    float my_shadow_var = 3.0;
                    my_shad<caret>
                }
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val lookups = myFixture.completeBasic()

        if (lookups == null) {
            myFixture.checkResult(
                """
                int my_shadow_var = 1;
                void main() {
                    int my_shadow_var = 2;
                    if (1) {
                        float my_shadow_var = 3.0;
                        my_shadow_var<caret>
                    }
                }
            """.trimIndent()
            )
        } else {
            val shadowVarCount = lookups.count { it.lookupString == "my_shadow_var" }
            assertEquals("Shadowed variable should appear exactly once in completion list", 1, shadowVarCount)
        }
    }

    fun testStructMemberCompletion() {
        val code = """
            struct Engine {
                int power;
            }

            struct Car {
                float speed;
                Engine engine;
            }

            void main() {
                Car myCar;
                // 'sp' should complete to 'speed'
                myCar.sp<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val lookups = myFixture.completeBasic()
        assertNull("Completion should auto-insert when only one match exists", lookups)

        myFixture.checkResult(
            """
            struct Engine {
                int power;
            }

            struct Car {
                float speed;
                Engine engine;
            }

            void main() {
                Car myCar;
                // 'sp' should complete to 'speed'
                myCar.speed<caret>
            }
        """.trimIndent()
        )

        val nestedCode = """
            struct Engine {
                int power;
            }

            struct Car {
                float speed;
                Engine engine;
            }

            void main() {
                Car myCar;
                // 'pow' should complete to 'power'
                myCar.engine.pow<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, nestedCode)
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            struct Engine {
                int power;
            }

            struct Car {
                float speed;
                Engine engine;
            }

            void main() {
                Car myCar;
                // 'pow' should complete to 'power'
                myCar.engine.power<caret>
            }
        """.trimIndent()
        )
    }

    fun testSwizzleCompletion() {
        val code = """
            void main() {
                vector pos = {1, 2, 3};
                // 'xy' should be in the completion list for vector
                pos.<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val lookups = myFixture.completeBasic()
        assertNotNull("Completion list should not be null for swizzles", lookups)

        val lookupStrings = lookups!!.map { it.lookupString }

        assertTrue("Completion should contain 'x'", lookupStrings.contains("x"))
        assertTrue("Completion should contain 'xy'", lookupStrings.contains("xy"))
        assertTrue("Completion should contain 'xyz'", lookupStrings.contains("xyz"))

        assertFalse("Completion should NOT contain 'w' for vector3", lookupStrings.contains("w"))
        assertFalse("Completion should NOT contain 'xyzw' for vector3", lookupStrings.contains("xyzw"))

        assertFalse("Completion should NOT contain local variables during dot access", lookupStrings.contains("pos"))
        assertFalse("Completion should NOT contain standard functions during dot access", lookupStrings.contains("abs"))
    }
}
