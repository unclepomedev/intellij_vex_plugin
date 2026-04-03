package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType

class VexConditionalCompilationTest : VexTestBase() {

    fun testIfdefActive() {
        myFixture.configureByText(
            VexFileType,
            """
            #define MACRO
            #ifdef MACRO
            int a = 1;
            #endif
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testIfdefInactive() {
        myFixture.configureByText(
            VexFileType,
            """
            #ifdef MACRO
            int a = 1;
            #endif
            float a = 2.0;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testIfndef() {
        myFixture.configureByText(
            VexFileType,
            """
            #ifndef MACRO
            int a = 1;
            #endif
            
            #define OTHER
            #ifndef OTHER
            int b = 1;
            #endif
            float b = 2.0;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testElseBranch() {
        myFixture.configureByText(
            VexFileType,
            """
            #define A
            #ifdef A
            int x = 1;
            #else
            float x = 2.0;
            #endif
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testNestedDirectives() {
        myFixture.configureByText(
            VexFileType,
            """
            #define OUTER
            #ifdef OUTER
            #ifdef INNER
            int a = 1;
            #else
            int a = 2;
            #endif
            #endif
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testInactiveFunctionNotResolved() {
        myFixture.configureByText(
            VexFileType,
            """
            #ifdef NEVER_DEFINED
            void foo() {}
            #endif
            
            void main() {
                <error descr="Unknown VEX function: 'foo'">foo</error>();
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }
}
