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

        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testVariableRedeclarationInSameScope() {
        myFixture.configureByText(
            VexFileType,
            """
            int myVar = 1;
            float <error descr="Variable 'myVar' is already defined in this scope">myVar</error> = 2.0;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
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
        myFixture.checkHighlighting(false, false, false, true)
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
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testValidVariableInDifferentStructs() {
        myFixture.configureByText(
            VexFileType,
            """
            struct A { int val; }
            struct B { int val; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testUnresolvedVariableIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            int a = 1;
            int b = a + <error descr="Unresolved variable: 'c'">c</error>;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
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
}
