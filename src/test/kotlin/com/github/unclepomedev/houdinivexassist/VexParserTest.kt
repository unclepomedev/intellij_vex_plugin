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

    fun testBrokenSyntaxCode() {
        val code = "if (a > b {"
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertTrue("Broken code should produce parse errors", hasErrors)
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
            for (int j = 0; j < 5; j++) {
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

    fun testForeachVariations() {
        val code = """
            // Simple form
            foreach (int val; myarray) {}
            foreach (val; myarray) {}
            
            // Enumerated form (Comma)
            foreach (int idx, int val; myarray) {}
            foreach (idx, val; myarray) {}
            
            // Enumerated form (Semicolon)
            foreach (int idx; int val; myarray) {}
            foreach (idx; val; myarray) {}
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("All foreach variations from official docs should be parsed", hasErrors)
    }

    fun testStructAndFunctions() {
        val code = """
            struct MyStruct {
                int a;
                float b = 1.0;
                vector c[] = {{1,0,0}, {0,1,0}};
                int myfunc -> function_name(int, float);
                void run -> runner();
            }
            
            void myFunc(int a; float b; vector c) {
                @P.x += a;
            }
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Struct and semicolon separated arguments should be parsed", hasErrors)
    }

    fun testBrokenStructSyntax() {
        val code = """
            struct Bad {
                int a
                float b = 1.0;
            }
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertTrue("Broken struct syntax should produce parse errors", hasErrors)
    }

    fun testBitwiseAndShiftOperators() {
        val code = """
            int a = 1 << 4;
            int b = a >> 2;
            int c = a & b | ~a ^ 0xFF;
            a <<= 1;
            b &= c;
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Bitwise and shift operators should be parsed", hasErrors)

        val cDecl = PsiTreeUtil.findChildrenOfType(
            file,
            com.github.unclepomedev.houdinivexassist.psi.VexDeclarationStatement::class.java
        )
            .find { it.text.startsWith("int c =") }
        assertNotNull("Could not find declaration for 'c'", cDecl)

        val orExpr = PsiTreeUtil.findChildOfType(
            cDecl,
            com.github.unclepomedev.houdinivexassist.psi.VexBitwiseOrExpr::class.java
        )
        assertNotNull("c initializer should be a bitwise OR expression", orExpr)

        val andExpr = PsiTreeUtil.getChildOfType(
            orExpr,
            com.github.unclepomedev.houdinivexassist.psi.VexBitwiseAndExpr::class.java
        )
        assertNotNull("Left child should be a bitwise AND expression", andExpr)
        assertEquals("a & b", andExpr!!.text)

        val xorExpr = PsiTreeUtil.getChildOfType(
            orExpr,
            com.github.unclepomedev.houdinivexassist.psi.VexBitwiseXorExpr::class.java
        )
        assertNotNull("Right child should be a bitwise XOR expression", xorExpr)
        assertEquals("~a ^ 0xFF", xorExpr!!.text)

        val notExpr =
            PsiTreeUtil.getChildOfType(xorExpr, com.github.unclepomedev.houdinivexassist.psi.VexPrefixExpr::class.java)
        assertNotNull("Left child of XOR should be a prefix expression (~a)", notExpr)
        assertEquals("~a", notExpr!!.text)
    }

    fun testAdvancedFunctionDefinitions() {
        val code = """
            export function void my_main() {
                @P = {0,0,0};
            }
            
            export int get_id() {
                return @ptnum;
            }
            
            function string get_name() {
                return "test";
            }
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Export and function modifiers should be parsed", hasErrors)
    }
}
