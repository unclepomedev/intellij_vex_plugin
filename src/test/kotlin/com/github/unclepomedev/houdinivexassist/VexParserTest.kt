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
            file, com.github.unclepomedev.houdinivexassist.psi.VexDeclarationStatement::class.java
        ).find { it.text.startsWith("int c =") }
        assertNotNull("Could not find declaration for 'c'", cDecl)

        val orExpr = PsiTreeUtil.findChildOfType(
            cDecl, com.github.unclepomedev.houdinivexassist.psi.VexBitwiseOrExpr::class.java
        )
        assertNotNull("c initializer should be a bitwise OR expression", orExpr)

        val andExpr = PsiTreeUtil.getChildOfType(
            orExpr, com.github.unclepomedev.houdinivexassist.psi.VexBitwiseAndExpr::class.java
        )
        assertNotNull("Left child should be a bitwise AND expression", andExpr)
        assertEquals("a & b", andExpr!!.text)

        val xorExpr = PsiTreeUtil.getChildOfType(
            orExpr, com.github.unclepomedev.houdinivexassist.psi.VexBitwiseXorExpr::class.java
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

    fun testIncludeDirective() {
        val code = """
            #include "math.vfl"
            #include 'utils.h'
            #include <kinefx.h>
            
            void main() {
                int a = 1;
            }
        """.trimIndent()

        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Include directives should be parsed without errors", hasErrors)

        val includes = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexIncludeDirective::class.java
        )
        assertEquals("Should find exactly 3 include directives", 3, includes.size)
    }

    fun testEmptyDictLiteral() {
        val code = "dict my_dict = {};"
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Empty dict literal should not produce parse errors", hasErrors)

        val dictLiterals = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexDictLiteral::class.java
        )
        assertEquals("Empty dict should produce one dict literal node", 1, dictLiterals.size)

        val vectorLiterals = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexVectorLiteral::class.java
        )
        assertEquals("Empty dict should not be parsed as vector literal", 0, vectorLiterals.size)
    }

    fun testDictLiteralWithEntries() {
        val code = """dict d = { "key1": 1, "key2": "value" };"""
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Dict literal with entries should not produce parse errors", hasErrors)

        val dictLiterals = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexDictLiteral::class.java
        )
        assertEquals(1, dictLiterals.size)
    }

    fun testNestedDictLiteral() {
        val code = """dict nested = { "a": { "b": 1 }, "c": { 1, 2, 3 } };"""
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Nested dict literal should not produce parse errors", hasErrors)
        val dictLiterals = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexDictLiteral::class.java
        )
        assertEquals("Should parse outer and inner dict literals", 2, dictLiterals.size)
        val vectorLiterals = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexVectorLiteral::class.java
        )
        assertEquals("Should parse { 1, 2, 3 } as vector literal", 1, vectorLiterals.size)
    }

    fun testBasicContextDef() {
        val code = "cvex my_cvex() { int a = 1; }"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse(PsiTreeUtil.hasErrorElements(file))

        val contextDefs = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexContextDef::class.java
        )
        assertEquals(1, contextDefs.size)
    }

    fun testContextDefWithParameters() {
        val code = """surface my_shader(float intensity; vector P) { vector color = {1,1,1}; }"""
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse(PsiTreeUtil.hasErrorElements(file))

        val contextDefs = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexContextDef::class.java
        )
        assertEquals(1, contextDefs.size)
    }

    fun testSopContextDef() {
        val code = "sop my_sop() { int id = @ptnum; }"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse(PsiTreeUtil.hasErrorElements(file))

        val contextDefs = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexContextDef::class.java
        )
        assertEquals(1, contextDefs.size)
    }


    fun testSimpleArrayAccess() {
        val code = "a[i];"
        val file = myFixture.configureByText(VexFileType, code)
        val errors = PsiTreeUtil.collectElements(file) { it is com.intellij.psi.PsiErrorElement }
        val errorMsg = errors.joinToString { (it as com.intellij.psi.PsiErrorElement).errorDescription }
        assertFalse("Simple array access should parse without errors: $errorMsg", errors.isNotEmpty())
        val arrayAccess = PsiTreeUtil.findChildOfType(
            file,
            com.github.unclepomedev.houdinivexassist.psi.VexArrayAccessExpr::class.java
        )
        assertNotNull("VexArrayAccessExpr node should be present", arrayAccess)
        val indexExpr = arrayAccess!!.exprList.getOrNull(1)
        assertNotNull("VexArrayAccessExpr should contain an index expression", indexExpr)
    }

    fun testModuloInArrayIndexMinimal() {
        val code = "a[i % n];"
        val file = myFixture.configureByText(VexFileType, code)
        val errors = PsiTreeUtil.collectElements(file) { it is com.intellij.psi.PsiErrorElement }
        val errorMsg = errors.joinToString { (it as com.intellij.psi.PsiErrorElement).errorDescription }
        assertFalse("Minimal modulo in array index should parse without errors: $errorMsg", errors.isNotEmpty())
        val arrayAccess = PsiTreeUtil.findChildOfType(
            file,
            com.github.unclepomedev.houdinivexassist.psi.VexArrayAccessExpr::class.java
        )
        assertNotNull("VexArrayAccessExpr node should be present", arrayAccess)
        val indexExpr = arrayAccess!!.exprList.getOrNull(1)
        assertNotNull("VexArrayAccessExpr should contain an index expression", indexExpr)
    }

    fun testModuloInArrayIndexInDeclaration() {
        val code = "int r = a[(i + 1) % n];"
        val file = myFixture.configureByText(VexFileType, code)
        val errors = PsiTreeUtil.collectElements(file) { it is com.intellij.psi.PsiErrorElement }
        val errorMsg = errors.joinToString { (it as com.intellij.psi.PsiErrorElement).errorDescription }
        assertFalse(
            "Modulo in array index inside declaration should parse without errors: $errorMsg",
            errors.isNotEmpty()
        )
        val arrayAccess = PsiTreeUtil.findChildOfType(
            file,
            com.github.unclepomedev.houdinivexassist.psi.VexArrayAccessExpr::class.java
        )
        assertNotNull("VexArrayAccessExpr node should be present", arrayAccess)
        val indexExpr = arrayAccess!!.exprList.getOrNull(1)
        assertNotNull("VexArrayAccessExpr should contain an index expression", indexExpr)
    }

    fun testArrayVariableDeclaration() {
        val code = "int pts[] = primpoints(0, prim_num);"
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Array variable declaration should parse without errors", hasErrors)
    }

    fun testModuloInArrayIndexParsesCorrectly() {
        val code = """
            vector get_prim_normal(int prim_num) {
                int pts[] = primpoints(0, prim_num);
                int n_pts = len(pts);
                vector n = set(0, 0, 0);

                for(int i = 0; i < n_pts; i++) {
                    vector p0 = point(0, "P", pts[i]);
                    vector p1 = point(0, "P", pts[(i + 1) % n_pts]);
                    n += cross(p0, p1);
                }

                if (length(n) < 0.0001) return set(0, 1, 0);
                return normalize(n);
            }
        """.trimIndent()
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse(
            "Modulo operator inside array index should parse without errors after fix",
            PsiTreeUtil.hasErrorElements(file)
        )
        val arrayAccesses = PsiTreeUtil.findChildrenOfType(
            file,
            com.github.unclepomedev.houdinivexassist.psi.VexArrayAccessExpr::class.java
        )
        assertTrue("VexArrayAccessExpr nodes should be present", arrayAccesses.isNotEmpty())
        arrayAccesses.forEach { arrayAccess ->
            assertNotNull(
                "Each VexArrayAccessExpr should contain an index expression",
                arrayAccess.exprList.getOrNull(1)
            )
        }
    }

    fun testAddInArrayIndex() {
        val code = "a[i + 1];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Add operator in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testSubtractInArrayIndex() {
        val code = "a[i - 1];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Subtract operator in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testMultiplyInArrayIndex() {
        val code = "a[i * 2];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Multiply operator in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testDivideInArrayIndex() {
        val code = "a[i / 2];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Divide operator in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testBitwiseAndInArrayIndex() {
        val code = "a[i & mask];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Bitwise AND in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testBitwiseOrInArrayIndex() {
        val code = "a[i | flags];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Bitwise OR in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testBitwiseXorInArrayIndex() {
        val code = "a[i ^ mask];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Bitwise XOR in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testLeftShiftInArrayIndex() {
        val code = "a[i << 2];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Left shift in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testRightShiftInArrayIndex() {
        val code = "a[i >> 2];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Right shift in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testEqualityInArrayIndex() {
        val code = "a[i == j];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Equality operator in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testRelationalInArrayIndex() {
        val code = "a[i < n];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse(
            "Relational operator in array index should parse without errors",
            PsiTreeUtil.hasErrorElements(file)
        )
    }

    fun testLogicalAndInArrayIndex() {
        val code = "a[i > 0 && i < n];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Logical AND in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testLogicalOrInArrayIndex() {
        val code = "a[i == 0 || i == n];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Logical OR in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testTernaryInArrayIndex() {
        val code = "a[cond ? i : j];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Ternary operator in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testComplexExprInArrayIndex() {
        val code = "a[(i + 1) * 2 - offset % n];"
        val file = myFixture.configureByText(VexFileType, code)
        assertFalse("Complex expression in array index should parse without errors", PsiTreeUtil.hasErrorElements(file))
    }

    fun testVectorLiteralStillWorks() {
        val code = "vector v = {1, 2, 3};"
        val file = myFixture.configureByText(VexFileType, code)
        val hasErrors = PsiTreeUtil.hasErrorElements(file)
        assertFalse("Vector literal should still parse correctly", hasErrors)

        val vectorLiterals = PsiTreeUtil.findChildrenOfType(
            file, com.github.unclepomedev.houdinivexassist.psi.VexVectorLiteral::class.java
        )
        assertEquals(1, vectorLiterals.size)
    }
}
