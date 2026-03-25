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

    fun testStructCompletion() {
        val code = """
            struct MyAwesomeStruct {
                int a;
            }
            
            void main() {
                MyAwes<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            struct MyAwesomeStruct {
                int a;
            }
            
            void main() {
                MyAwesomeStruct<caret>
            }
        """.trimIndent()
        )
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

    fun testPrimitiveTypeCompletion() {
        val code = """
            void main() {
                vec<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val lookups = myFixture.completeBasic()

        assertNotNull("Completion list should not be null", lookups)
        val lookupStrings = lookups!!.map { it.lookupString }

        assertTrue("Completion should contain 'vector'", lookupStrings.contains("vector"))
        assertTrue("Completion should contain 'vector2'", lookupStrings.contains("vector2"))
        assertTrue("Completion should contain 'vector4'", lookupStrings.contains("vector4"))

        assertFalse("Completion should NOT contain 'int'", lookupStrings.contains("int"))
        assertFalse("Completion should NOT contain 'float'", lookupStrings.contains("float"))
    }

    fun testPrimitiveTypeCompletionInsertsSpaceNormally() {
        val code1 = """
            void main() {
                in<caret>my_var;
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code1)

        var targetLookup = myFixture.completeBasic()?.find { it.lookupString == "int" }
        assertNotNull(targetLookup)
        myFixture.lookup.currentItem = targetLookup
        myFixture.type('\n')

        myFixture.checkResult(
            """
            void main() {
                int <caret>my_var;
            }
        """.trimIndent()
        )

        val code2 = """
            void myFunc(in<caret>, float b) {}
        """.trimIndent()
        myFixture.configureByText(VexFileType, code2)

        targetLookup = myFixture.completeBasic()?.find { it.lookupString == "int" }
        assertNotNull(targetLookup)
        myFixture.lookup.currentItem = targetLookup
        myFixture.type('\n')

        myFixture.checkResult(
            """
            void myFunc(int <caret>, float b) {}
        """.trimIndent()
        )
    }

    fun testPrimitiveTypeCompletionDoesNotInsertSpaceBeforeBrackets() {
        val code1 = """
            void main() {
                vector pos = vec<caret>(1.0, 2.0, 3.0);
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code1)

        var targetLookup = myFixture.completeBasic()?.find { it.lookupString == "vector" }
        assertNotNull(targetLookup)
        myFixture.lookup.currentItem = targetLookup
        myFixture.type('\n')

        myFixture.checkResult(
            """
            void main() {
                vector pos = vector<caret>(1.0, 2.0, 3.0);
            }
        """.trimIndent()
        )

        val code2 = """
            void main() {
                vector a = vector(floa<caret>);
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code2)

        targetLookup = myFixture.completeBasic()?.find { it.lookupString == "float" }
        assertNotNull(targetLookup)
        myFixture.lookup.currentItem = targetLookup
        myFixture.type('\n')

        myFixture.checkResult(
            """
            void main() {
                vector a = vector(float<caret>);
            }
        """.trimIndent()
        )

        val code3 = """
            void main() {
                floa<caret>[] my_array;
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code3)

        targetLookup = myFixture.completeBasic()?.find { it.lookupString == "float" }
        assertNotNull(targetLookup)
        myFixture.lookup.currentItem = targetLookup
        myFixture.type('\n')

        myFixture.checkResult(
            """
            void main() {
                float<caret>[] my_array;
            }
        """.trimIndent()
        )
    }

    fun testZeroArgFunctionInsert() {
        val code = """
            void my_zero_arg() {}
            void my_zero_dummy() {}
            void main() {
                my_zero<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val targetLookup = myFixture.completeBasic()?.find { it.lookupString == "my_zero_arg" }
        assertNotNull("Lookup list should not be null", targetLookup)
        myFixture.lookup.currentItem = targetLookup

        myFixture.type('\n')

        myFixture.checkResult(
            """
            void my_zero_arg() {}
            void my_zero_dummy() {}
            void main() {
                my_zero_arg()<caret>
            }
        """.trimIndent()
        )
    }

    fun testArgFunctionInsert() {
        val code = """
            void my_arg_func(int a) {}
            void my_arg_dummy() {}
            void main() {
                my_arg_<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val targetLookup = myFixture.completeBasic()?.find { it.lookupString == "my_arg_func" }
        assertNotNull("Lookup list should not be null", targetLookup)
        myFixture.lookup.currentItem = targetLookup

        myFixture.type('\n')

        myFixture.checkResult(
            """
            void my_arg_func(int a) {}
            void my_arg_dummy() {}
            void main() {
                my_arg_func(<caret>)
            }
        """.trimIndent()
        )
    }

    fun testFunctionInsertWithParenTyped() {
        val code = """
            void my_arg_func(int a) {}
            void my_arg_dummy() {}
            void main() {
                my_arg_<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val targetLookup = myFixture.completeBasic()?.find { it.lookupString == "my_arg_func" }
        assertNotNull("Lookup list should not be null", targetLookup)
        myFixture.lookup.currentItem = targetLookup

        myFixture.type('(')

        myFixture.checkResult(
            """
            void my_arg_func(int a) {}
            void my_arg_dummy() {}
            void main() {
                my_arg_func(<caret>)
            }
        """.trimIndent()
        )
    }

    fun testFunctionInsertWithExistingParens() {
        val code = """
            void my_arg_func(int a) {}
            void my_arg_dummy() {}
            void main() {
                my_arg_<caret>()
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val targetLookup = myFixture.completeBasic()?.find { it.lookupString == "my_arg_func" }
        assertNotNull("Lookup list should not be null", targetLookup)
        myFixture.lookup.currentItem = targetLookup

        myFixture.type('\n')

        myFixture.checkResult(
            """
            void my_arg_func(int a) {}
            void my_arg_dummy() {}
            void main() {
                my_arg_func(<caret>)
            }
        """.trimIndent()
        )
    }

    fun testZeroArgFunctionInsertWithExistingParens() {
        val code = """
            void my_zero_arg() {}
            void my_zero_dummy() {}
            void main() {
                my_zero<caret>()
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)

        val targetLookup = myFixture.completeBasic()?.find { it.lookupString == "my_zero_arg" }
        assertNotNull("Lookup item should not be null", targetLookup)
        myFixture.lookup.currentItem = targetLookup

        myFixture.type('\n')

        myFixture.checkResult(
            """
            void my_zero_arg() {}
            void my_zero_dummy() {}
            void main() {
                my_zero_arg()<caret>
            }
        """.trimIndent()
        )
    }

    fun testIncludeCompletion() {
        myFixture.addFileToProject(
            "my_lib.vfl",
            """
            void my_lib_func() { }
            int my_lib_var = 123;
            struct my_lib_struct { int a; }
            """.trimIndent()
        )
        val code = """
            #include "my_lib.vfl"
            void main() {
                my_lib_<caret>
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion list should not be null", lookups)
        val lookupStrings = lookups!!.map { it.lookupString }
        assertTrue("Completion should contain 'my_lib_func'", lookupStrings.contains("my_lib_func"))
        assertTrue("Completion should contain 'my_lib_var'", lookupStrings.contains("my_lib_var"))
        assertTrue("Completion should contain 'my_lib_struct'", lookupStrings.contains("my_lib_struct"))
    }
}
