package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.psi.util.PsiTreeUtil

class VexTypeInferenceTest : VexTestBase() {

    fun testInferLiterals() {
        val code = """
            void main() {
                int v1 = 1;
                float v2 = 1.0;
                float v3 = 1e-5;
                string v4 = "hello";
                vector v5 = {1, 2, 3};
                float v6 = (1.0);
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()
        val exprs = declItems.mapNotNull { it.expr }
        assertEquals(6, exprs.size)

        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[0]))      // 1
        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[1]))    // 1.0
        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[2]))    // 1e-5
        assertEquals(VexType.StringType, VexTypeInference.inferType(exprs[3]))   // "hello"
        assertEquals(VexType.VectorType, VexTypeInference.inferType(exprs[4]))   // {1, 2, 3}
        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[5]))    // (1.0)
    }

    fun testInferAttributes() {
        val code = """
            void main() {
                @P;
                @ptnum;
                v@my_vec;
                f@my_float;
                s@my_str;
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val exprs = PsiTreeUtil.findChildrenOfType(file, VexPrimaryExpr::class.java).toList()
        assertEquals(5, exprs.size)

        assertEquals(VexType.VectorType, VexTypeInference.inferType(exprs[0])) // @P
        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[1]))    // @ptnum
        assertEquals(VexType.VectorType, VexTypeInference.inferType(exprs[2])) // v@my_vec
        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[3]))  // f@my_float
        assertEquals(VexType.StringType, VexTypeInference.inferType(exprs[4])) // s@my_str
    }

    fun testInferVariableReference() {
        val code = """
            void main() {
                matrix myVar = 1;
                myVar;
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val exprs = PsiTreeUtil.findChildrenOfType(file, VexPrimaryExpr::class.java).toList()
        assertEquals(2, exprs.size)

        assertEquals("myVar", exprs[1].text)
        assertEquals(VexType.MatrixType, VexTypeInference.inferType(exprs[1]))
    }

    fun testInferCallExpr() {
        val code = """
            string myFunc(int a) { return "test"; }
            
            void main() {
                myFunc(1);
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val callExprs = PsiTreeUtil.findChildrenOfType(file, VexCallExpr::class.java).toList()
        assertEquals(1, callExprs.size)

        assertEquals(VexType.StringType, VexTypeInference.inferType(callExprs[0]))
    }

    fun testInferOperators() {
        val code = """
            void main() {
                // int + float -> float
                float v1 = 1 + 2.0;
                
                // vector * float -> vector
                vector v2 = {1, 2, 3} * 0.5;
                
                // float < float -> int
                int v3 = 1.0 < 2.0;
                
                // string + int -> string
                string v4 = "Value: " + 100;
                
                // += represents the type of the left operand
                int a = 1;
                int v5 = (a += 2);
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()

        val targetExprs = listOf(
            declItems[0].expr, // 1 + 2.0
            declItems[1].expr, // {1, 2, 3} * 0.5
            declItems[2].expr, // 1.0 < 2.0
            declItems[3].expr, // "Value: " + 100
            declItems[5].expr  // (a += 2)
        )

        assertEquals(VexType.FloatType, VexTypeInference.inferType(targetExprs[0]))
        assertEquals(VexType.VectorType, VexTypeInference.inferType(targetExprs[1]))
        assertEquals(VexType.IntType, VexTypeInference.inferType(targetExprs[2]))
        assertEquals(VexType.StringType, VexTypeInference.inferType(targetExprs[3]))
        assertEquals(VexType.IntType, VexTypeInference.inferType(targetExprs[4]))
    }
}
