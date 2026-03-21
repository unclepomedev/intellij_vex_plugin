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

    fun testFunctionOverloadReference() {
        myFixture.configureByText(
            VexFileType, """
            void myFunc(int a) {}
            
            void myFunc(int a, float b) {} 

            void main() {
                myF<caret>unc(1, 2.0);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)

        val funcDef = resolved as VexFunctionDef
        assertEquals("myFunc", funcDef.identifier.text)

        val paramCount = funcDef.parameterListDef?.parameterDefList?.size ?: 0
        assertEquals("Should resolve to the overload with 2 parameters", 2, paramCount)
    }

    fun testFunctionOverloadReferenceByTypeSignature() {
        myFixture.configureByText(
            VexFileType, """
            void process(int a) {}      // candidate 1 (Int)
            void process(string a) {}   // candidate 2 (String)
            void process(float a) {}    // candidate 3 (Float)

            void main() {
                // This should jump to the string overload, not the first one defined.
                proce<caret>ss("hello");
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)

        val funcDef = resolved as VexFunctionDef
        assertEquals("process", funcDef.identifier.text)

        // Ensure we jumped to the correct overload by checking the parameter type
        val paramType = funcDef.parameterListDef?.parameterDefList?.firstOrNull()?.typeRef?.text
        assertEquals("Should resolve to the overload with string parameter", "string", paramType)
    }

    fun testFunctionOverloadReferenceWithImplicitCast() {
        myFixture.configureByText(
            VexFileType, """
            void set(int a, int b) {}       // candidate 1
            void set(vector a, vector b) {} // candidate 2

            void main() {
                // This should jump to the vector overload due to implicit casting from float
                s<caret>et({1,2,3}, 2.0);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)

        val funcDef = resolved as VexFunctionDef
        assertEquals("set", funcDef.identifier.text)

        // Ensure we jumped to the correct overload by checking the parameter type
        val paramType = funcDef.parameterListDef?.parameterDefList?.firstOrNull()?.typeRef?.text
        assertEquals("Should resolve to the overload with vector parameter via implicit cast", "vector", paramType)
    }

    fun testVariableRename() {
        myFixture.configureByText(
            VexFileType, """
            function void test() {
                int oldVar = 1;
                <caret>oldVar = 2;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNotNull("Resolve failed", ref.resolve())

        myFixture.renameElementAtCaret("newVarName")

        myFixture.checkResult(
            """
            function void test() {
                int newVarName = 1;
                newVarName = 2;
            }
        """.trimIndent()
        )
    }

    fun testFunctionParameterRename() {
        myFixture.configureByText(
            VexFileType, """
            void myFunc(int p) {
                <caret>p = 10;
            }
        """.trimIndent()
        )

        myFixture.renameElementAtCaret("paramNew")

        myFixture.checkResult(
            """
            void myFunc(int paramNew) {
                paramNew = 10;
            }
        """.trimIndent()
        )
    }

    fun testFunctionRename() {
        myFixture.configureByText(
            VexFileType, """
            void oldName() {
            }

            void main() {
                old<caret>Name();
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNotNull("Resolve failed", ref.resolve())

        myFixture.renameElementAtCaret("newName")

        myFixture.checkResult(
            """
            void newName() {
            }

            void main() {
                newName();
            }
        """.trimIndent()
        )
    }
}
