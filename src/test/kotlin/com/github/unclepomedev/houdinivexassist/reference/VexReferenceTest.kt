package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.psi.VexDeclarationItem
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionDef
import com.github.unclepomedev.houdinivexassist.psi.VexParameterDef

class VexReferenceTest : VexTestBase() {

    fun testVariableReference() {
        myFixture.configureByText(
            VexFileType, """
            function void test() {
                int myVar = 1;
                m<caret>yVar = 2;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Reference should be resolved", resolved)
        assertTrue("Resolved element should be a declaration", resolved is VexDeclarationItem)
    }

    fun testParameterReference() {
        myFixture.configureByText(
            VexFileType, """
            function void test(int param1) {
                p<caret>aram1 = 2;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Reference should be resolved", resolved)
        assertTrue("Resolved element should be a parameter definition", resolved is VexParameterDef)
    }

    fun testFunctionReference() {
        myFixture.configureByText(
            VexFileType, """
            void myTargetFunction(int a) {
            }

            void main() {
                myTarg<caret>etFunction(1);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)
        assertEquals("myTargetFunction", (resolved as VexFunctionDef).identifier.text)
    }
}
