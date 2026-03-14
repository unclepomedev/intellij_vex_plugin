package com.github.unclepomedev.houdinivexassist

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.intellij.psi.util.PsiTreeUtil

class VexParserTest : VexTestBase() {

    fun testEmptyFile() {
        val file = myFixture.configureByText(VexFileType, "")
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Empty file should not produce parse errors", hasErrors)
    }

    fun testStandardCode() {
        val code = """
            int a = 1;
            vector pos = @P;
            void update() {
                @P = pos + {0, 1, 0};
            }
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Standard code should not produce parse errors", hasErrors)
    }

    // TODO: this is temporal error-free behavior
    fun testBrokenSyntaxCode() {
        val code = "if (a > b {"
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Broken code should not produce parse errors", hasErrors)
    }

    fun testIfStatementAndMemberAccess() {
        val code = "if (@P.y > 0) { v@Cd = {1, 0, 0}; }"
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("if statement and member access should be parsed", hasErrors)
    }

    fun testLoops() {
        val code = """
            for (i = 0; i < 10; i++) {
                if (i % 2 == 0) continue;
                break;
            }
            foreach (int ptnum; pts) {
                @P.y++;
            }
            while (a < b) { 
                ++a; 
            }
            do {
                b--;
            } while (b > 0);
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Loop statements should not produce parse errors", hasErrors)
    }

    fun testComplexExpressions() {
        val code = """
            float val = (a + b) * c / -d;
            int flag = (val >= 0.5 && val <= 1.0) ? 1 : 0;
            int is_valid = !flag || a != b;
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Complex expressions (ternary, logical, prefix) should be parsed", hasErrors)
    }

    fun testFunctionCalls() {
        val code = """
            float dist = distance(@P, {0,0,0});
            f[]@colors = array({1,0,0}, {0,1,0});
            ch("scale");
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Function calls should not produce parse errors", hasErrors)
    }

    fun testMultipleDeclarations() {
        val code = """
            int a = 1, b = 2, c;
            float x, y = 3.14;
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Multiple variable declarations should be parsed", hasErrors)
    }

    fun testIncrementDecrement() {
        val code = """
            int a = 0;
            int b = a++;
            int c = --b;
            @P.x++;
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Increment and decrement should be parsed", hasErrors)
    }
}
