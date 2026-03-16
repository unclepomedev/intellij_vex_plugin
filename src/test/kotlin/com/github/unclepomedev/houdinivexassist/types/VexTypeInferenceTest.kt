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
                
                // bitwise and / shift stay int
                int v6 = 1 & 2;
                int v7 = 1 << 2;
            }
        """.trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()
        assertEquals(8, declItems.size)

        val targetExprs = listOf(
            declItems[0].expr, // 1 + 2.0
            declItems[1].expr, // {1, 2, 3} * 0.5
            declItems[2].expr, // 1.0 < 2.0
            declItems[3].expr, // "Value: " + 100
            declItems[5].expr, // (a += 2)
            declItems[6].expr, // 1 & 2
            declItems[7].expr  // 1 << 2
        )

        assertEquals(VexType.FloatType, VexTypeInference.inferType(targetExprs[0]))
        assertEquals(VexType.VectorType, VexTypeInference.inferType(targetExprs[1]))
        assertEquals(VexType.IntType, VexTypeInference.inferType(targetExprs[2]))
        assertEquals(VexType.StringType, VexTypeInference.inferType(targetExprs[3]))
        assertEquals(VexType.IntType, VexTypeInference.inferType(targetExprs[4]))
        assertEquals(VexType.IntType, VexTypeInference.inferType(targetExprs[5]))
        assertEquals(VexType.IntType, VexTypeInference.inferType(targetExprs[6]))
    }

    fun testOperatorTypePromotionRules() {
        val code = """
            void main() {
                // Valid Operations
                float v1 = 1 + 2.0;          // ADDITIVE: int + float -> float
                vector v2 = {1,2,3} * 0.5;   // MULTIPLICATIVE: vector * float -> vector
                string v3 = "a" + "b";       // ADDITIVE: string + string -> string
                string v4 = "a" + 1;         // ADDITIVE: string + int -> string
                int v5 = 1 << 2;             // SHIFT: int << int -> int
                int v6 = 1 & 2;              // BITWISE: int & int -> int
                
                // Invalid Operations
                int inv1 = "x" * 2;          // MULTIPLICATIVE: string * int -> UnknownType
                int inv2 = 1 << 2.0;         // SHIFT: int << float -> UnknownType
                int inv3 = 2.0 & 1.0;        // BITWISE: float & float -> UnknownType
            }
        """.trimIndent()

        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()
        val exprs = declItems.mapNotNull { it.expr }
        assertEquals(9, exprs.size)

        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[0]))    // v1: 1 + 2.0
        assertEquals(VexType.VectorType, VexTypeInference.inferType(exprs[1]))   // v2: {1,2,3} * 0.5
        assertEquals(VexType.StringType, VexTypeInference.inferType(exprs[2]))   // v3: "a" + "b"
        assertEquals(VexType.StringType, VexTypeInference.inferType(exprs[3]))   // v4: "a" + 1
        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[4]))      // v5: 1 << 2
        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[5]))      // v6: 1 & 2

        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[6]))  // inv1: "x" * 2
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[7]))  // inv2: 1 << 2.0
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[8]))  // inv3: 2.0 & 1.0
    }

    fun testChainedOperatorExpressions() {
        val code = """
            void main() {
                int c1 = 1 + 2 + 3;
                int c2 = 1 + 2 - 3;
                float c3 = 1 + 2.0 + 3;
                float c4 = 1.0 * 2.0 / 3.0;
                vector c5 = {1,2,3} * 0.5 + {4,5,6};
            }
        """.trimIndent()

        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()
        val exprs = declItems.mapNotNull { it.expr }
        assertEquals(5, exprs.size)

        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[0]))      // 1 + 2 + 3 -> int
        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[1]))      // 1 + 2 - 3 -> int
        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[2]))    // 1 + 2.0 + 3 -> float
        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[3]))    // 1.0 * 2.0 / 3.0 -> float
        assertEquals(VexType.VectorType, VexTypeInference.inferType(exprs[4]))   // {1,2,3} * 0.5 + {4,5,6} -> vector
    }

    fun testStrictOperatorRulesAndPropagation() {
        val code = """
            void main() {
                string v1 = "a" + "b";       // allowed -> string
                string v2 = "a" - "b";       // not allowed -> UnknownType
                
                // propagate UnknownType
                int v3 = ("x" * 2) + 1;      // -> UnknownType

                int a = 1;
                int v4 = (a += 2);           // allowed -> int
                int v5 = (a <<= 2.0);        // not allowed float shift -> UnknownType

                string s = "test";
                string v6 = (s += "!");      // allowed -> string
                string v7 = (s -= "?");      // not allowed -> UnknownType
            }
        """.trimIndent()

        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()
        val exprs = declItems.mapNotNull { it.expr }
        assertEquals(9, exprs.size)

        assertEquals(VexType.StringType, VexTypeInference.inferType(exprs[0]))
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[1]))

        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[2]))

        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[4]))
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[5]))
        assertEquals(VexType.StringType, VexTypeInference.inferType(exprs[7]))
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[8]))
    }

    fun testInferMemberAccessAndSwizzling() {
        val code = """
            void main() {
                vector pos = {1, 2, 3};
                
                float v1 = pos.x;           // length 1 -> float
                vector2 v2 = pos.xy;        // length 2 -> vector2
                vector v3 = pos.zyx;        // length 3 -> vector
                vector4 v4 = pos.xyzw;      // length 4 -> vector4
                
                // Type chaining from attributes (@P is inferred as a vector type, and its .y becomes a float type)
                float v5 = @P.y;
                vector2 v6 = @uv.xy;
                
                // invalid
                int a = 1;
                float inv1 = a.x; 
                vector inv2 = pos.foo;
                float inv3 = pos.q;
                
                // additional
                matrix m = 1;
                float m1 = m.x;             // matrix swizzle length 1 -> float
                vector2 m2 = m.xy;          // matrix swizzle length 2 -> vector2
                vector4 m4 = m.rgba;        // matrix swizzle length 4 -> vector4
                float minv = m.qq;          // invalid swizzle char -> unknown
            }
        """.trimIndent()

        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()

        val exprs = declItems.mapNotNull { it.expr }
        assertEquals(16, exprs.size)

        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[1]))
        assertEquals(VexType.Vector2Type, VexTypeInference.inferType(exprs[2]))
        assertEquals(VexType.VectorType, VexTypeInference.inferType(exprs[3]))
        assertEquals(VexType.Vector4Type, VexTypeInference.inferType(exprs[4]))

        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[5]))
        assertEquals(VexType.Vector2Type, VexTypeInference.inferType(exprs[6]))

        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[8]))
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[9]))
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[10]))

        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[12]))
        assertEquals(VexType.Vector2Type, VexTypeInference.inferType(exprs[13]))
        assertEquals(VexType.Vector4Type, VexTypeInference.inferType(exprs[14]))
        assertEquals(VexType.UnknownType, VexTypeInference.inferType(exprs[15]))
    }

    fun testInferUnaryExpressions() {
        val code = """
            void main() {
                float f = 1.0;
                int i = 5;
                vector v = {1, 2, 3};
                
                int v1 = !f;
                int v2 = ~i;
                
                float v3 = -f;
                vector v4 = -v;
                
                float v5 = ++f;
                int v6 = i--;
            }
        """.trimIndent()

        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()

        val exprs = declItems.mapNotNull { it.expr }
        assertEquals(9, exprs.size)

        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[3])) // !f
        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[4])) // ~i

        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[5])) // -f
        assertEquals(VexType.VectorType, VexTypeInference.inferType(exprs[6])) // -v

        assertEquals(VexType.FloatType, VexTypeInference.inferType(exprs[7])) // ++f
        assertEquals(VexType.IntType, VexTypeInference.inferType(exprs[8]))   // i--
    }
}
