package com.github.unclepomedev.houdinivexassist

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.intellij.psi.PsiErrorElement
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

    fun testBrokenSyntaxCode() {
        val code = "if (a > b {"
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Broken code should not produce parse errors", hasErrors)
    }
}
