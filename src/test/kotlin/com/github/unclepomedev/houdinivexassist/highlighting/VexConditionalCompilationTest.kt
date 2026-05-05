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

    fun testIfElif() {
        myFixture.configureByText(
            VexFileType,
            """
            #ifdef NEVER_DEFINED
            int x = 1;
            #elif 1
            int x = 2;
            #else
            float x = 3.0;
            #endif
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testUndefAndInlineComment() {
        myFixture.configureByText(
            VexFileType,
            """
            #define MACRO
            #undef MACRO
            #ifdef MACRO
            int a = 1;
            #else // trailing
            float a = 2.0;
            #endif /* done */
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

    fun testMacroInInactiveBranchDoesNotAffectActiveBranch() {
        myFixture.configureByText(
            VexFileType,
            """
            #ifdef NEVER
            #define SHOULD_BE_INACTIVE
            #endif
            
            #ifdef SHOULD_BE_INACTIVE
            int a = 1;
            #endif
            int a = 2;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testMacroDefinedInInactiveBranchShouldNotBeFoundInLaterActiveBranch() {
        myFixture.configureByText(
            VexFileType,
            """
            #if 0
            #define INACTIVE_MACRO
            #endif
            
            #ifdef INACTIVE_MACRO
            int a = 1;
            #endif
            
            #define ACTIVE_MACRO
            #ifdef ACTIVE_MACRO
            int a = 1;
            #endif
            
            int <error descr="Variable 'a' is already defined in this scope">a</error> = 2;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testNestedIfdefCorrectlyEvaluated() {
        myFixture.configureByText(
            VexFileType,
            """
            #define OUTER
            #ifdef OUTER
              #define INNER
              #ifdef INNER
                int x = 1;
              #endif
            #endif
            
            void main() {
                int y = x;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testPartialMacroMatchShouldNotShortCircuit() {
        myFixture.configureByText(
            VexFileType,
            """
            #if 0 && defined(FOO)
            int a = 1;
            #endif
            int a = 2;
            int <error descr="Variable 'a' is already defined in this scope">a</error> = 3;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testNonVexIncludeMacroDiscovery() {
        myFixture.addFileToProject("header.h", "#define FROM_HEADER")
        myFixture.configureByText(
            VexFileType,
            """
            #include "header.h"
            #ifdef FROM_HEADER
            int a = 1;
            #endif
            int <error descr="Variable 'a' is already defined in this scope">a</error> = 2;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testIncludeDoesNotLeakInactiveRangesToParent() {
        myFixture.addFileToProject("header.vfl", "#if 0\n#endif")
        myFixture.configureByText(
            VexFileType,
            """
            #include "header.vfl"
            int a = 1;
            int <error descr="Variable 'a' is already defined in this scope">a</error> = 2;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }
}
