package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType

class VexAnnotatorTest : VexTestBase() {

    fun testValidFunctionIsNotHighlightedAsError() {
        myFixture.configureByText(VexFileType, "float d = distance(p1, p2);")
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testUnknownFunctionIsHighlightedAsError() {
        myFixture.configureByText(
            VexFileType,
            "float d = <error descr=\"Unknown VEX function: 'hogehoge'\">hogehoge</error>(p1, p2);"
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testLocalFunctionIsNotHighlightedAsError() {
        myFixture.configureByText(
            VexFileType,
            """
            void myLocalFunc() {
                // do something
            }
            
            float d = myLocalFunc();
            """.trimIndent()
        )

        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableRedeclarationInSameScope() {
        myFixture.configureByText(
            VexFileType,
            """
            int myVar = 1;
            float <error descr="Variable 'myVar' is already defined in this scope">myVar</error> = 2.0;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableRedeclarationWithParameter() {
        myFixture.configureByText(
            VexFileType,
            """
            void myFunc(int myParam) {
                float <error descr="Variable 'myParam' is already defined as a parameter">myParam</error> = 1.0;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testValidVariableShadowingInDifferentScope() {
        myFixture.configureByText(
            VexFileType,
            """
            int myVar = 1;
            if (myVar > 0) {
                int myVar = 2;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testValidVariableInDifferentStructs() {
        myFixture.configureByText(
            VexFileType,
            """
            struct A { int val; }
            struct B { int val; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testUnresolvedVariableIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            int a = 1;
            int b = a + <error descr="Unresolved variable: 'c'">c</error>;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testResolvedVariableIsNotHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            int outerVar = 1;
            void myFunc(int paramVar) {
                if (outerVar > 0) {
                    int innerVar = outerVar + paramVar;
                    @P.x += innerVar;
                }
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableConflictsWithStandardFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            float <error descr="Variable name 'distance' conflicts with a standard VEX function">distance</error> = 1.0;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableConflictsWithLocalFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            void myLocalFunc() {}
            
            void main() {
                int <error descr="Variable name 'myLocalFunc' conflicts with a local function">myLocalFunc</error> = 1;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionConflictsWithStandardFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            void <error descr="Function name 'normalize' conflicts with a standard VEX function">normalize</error>(vector v) {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionConflictsWithStruct() {
        myFixture.configureByText(
            VexFileType,
            """
            struct MyData { int a; }
            
            void <error descr="Function name 'MyData' conflicts with a struct definition">MyData</error>() {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionExactOverloadConflict() {
        myFixture.configureByText(
            VexFileType,
            """
            void myFunc(int a) {}
            
            void myFunc(int a, float b) {}
            
            void <error descr="Function 'myFunc' with 1 parameters is already defined">myFunc</error>(float a) {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStructConflictsWithStruct() {
        myFixture.configureByText(
            VexFileType,
            """
            struct MyData { int a; }
            struct <error descr="Struct 'MyData' is already defined">MyData</error> { float b; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStructConflictsWithStandardFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            struct <error descr="Struct name 'length' conflicts with a standard VEX function">length</error> { int val; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStructConflictsWithLocalFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            void myProc() {}
            
            struct <error descr="Struct name 'myProc' conflicts with a local function">myProc</error> { int val; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testForwardDeclaredFunctionCall() {
        myFixture.configureByText(
            VexFileType,
            """
        float d = myLaterFunc();
        
        void myLaterFunc() {}
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }
}
