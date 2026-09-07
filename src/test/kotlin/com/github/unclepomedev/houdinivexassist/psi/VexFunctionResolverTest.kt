package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeExtractor
import com.intellij.psi.util.PsiTreeUtil

class VexFunctionResolverTest : VexTestBase() {

    fun testParseApiArgType() {
        assertEquals(VexType.IntType, VexFunctionResolver.parseApiArgType("int"))
        assertEquals(VexType.FloatType, VexFunctionResolver.parseApiArgType("float val"))
        assertEquals(VexType.StringType, VexFunctionResolver.parseApiArgType("string &str"))
        assertEquals(VexType.VectorType, VexFunctionResolver.parseApiArgType("vector3 pos"))
        assertEquals(VexType.MatrixType, VexFunctionResolver.parseApiArgType("matrix4 mat"))
        assertEquals(
            VexType.ArrayType(VexType.IntType),
            VexFunctionResolver.parseApiArgType("int[] values"),
        )
        assertEquals(
            VexType.ArrayType(VexType.FloatType),
            VexFunctionResolver.parseApiArgType("float arr[]"),
        )
        assertEquals(
            VexType.StructType("MyStruct"),
            VexFunctionResolver.parseApiArgType("struct MyStruct s"),
        )
        assertEquals(
            VexType.ArrayType(VexType.StructType("MyStruct")),
            VexFunctionResolver.parseApiArgType("struct MyStruct[] items"),
        )
        assertEquals(VexType.FloatType, VexFunctionResolver.parseApiArgType("const float x"))
        assertEquals(VexType.IntType, VexFunctionResolver.parseApiArgType("export int result"))
        assertEquals(VexType.UnknownType, VexFunctionResolver.parseApiArgType(""))
        assertEquals(VexType.UnknownType, VexFunctionResolver.parseApiArgType("const export"))
    }

    fun testResolveParameterTypesForLocalFunction() {
        val code =
            """
            void myFunc(int a, string b[]) {}
            void main() {
                myFunc(10, {});
            }
            """
                .trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val callExpr = PsiTreeUtil.findChildrenOfType(file, VexCallExpr::class.java).first()

        val paramTypes = VexFunctionResolver.resolveParameterTypes(callExpr)
        assertNotNull(paramTypes)
        assertEquals(listOf(VexType.IntType, VexType.ArrayType(VexType.StringType)), paramTypes)
    }

    fun testResolveParameterNamesForLocalFunction() {
        val code =
            """
            void compute(float weight, vector offset, int count) {}
            void main() {
                compute(1.0, {0, 1, 0}, 5);
            }
            """
                .trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val callExpr = PsiTreeUtil.findChildrenOfType(file, VexCallExpr::class.java).first()

        val paramNames = VexFunctionResolver.resolveParameterNames(callExpr)
        assertEquals(listOf("weight", "offset", "count"), paramNames)
    }

    fun testResolveParameterTypesReturnsNullWhenLocalFunctionMissingParameterList() {
        val code =
            """
            void brokenFunc {
                brokenFunc();
            }
            """
                .trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val callExpr = PsiTreeUtil.findChildOfType(file, VexCallExpr::class.java)
        assertNotNull(callExpr)

        val paramTypes = VexFunctionResolver.resolveParameterTypes(callExpr!!)
        assertNull("Missing parameter list should return null, not empty list", paramTypes)
    }

    fun testResolveFunctionByExactSignature() {
        val code =
            """
            void calc(int a) {}
            void calc(float a) {}
            void calc(vector a) {}
            void main() {
                calc(1.0);
            }
            """
                .trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val callExpr = PsiTreeUtil.findChildOfType(file, VexCallExpr::class.java)!!

        val resolved =
            VexFunctionResolver.resolveFunction(
                element = callExpr,
                functionName = "calc",
                argTypes = listOf(VexType.FloatType),
            )
        assertNotNull(resolved)
        assertTrue(resolved is VexFunctionDef)
        val funcDef = resolved as VexFunctionDef
        val paramType =
            funcDef.parameterListDef?.parameterDefList?.firstOrNull()?.let {
                VexTypeExtractor.extractType(it)
            }
        assertEquals(VexType.FloatType, paramType)
    }

    fun testResolveFunctionWithImplicitTypePromotion() {
        val code =
            """
            void handle(string s) {}
            void handle(float f) {}
            void main() {
                handle(10); // int can be assigned to float
            }
            """
                .trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile
        val callExpr = PsiTreeUtil.findChildOfType(file, VexCallExpr::class.java)!!

        val resolved =
            VexFunctionResolver.resolveFunction(
                element = callExpr,
                functionName = "handle",
                argTypes = listOf(VexType.IntType),
            )
        assertNotNull(resolved)
        assertTrue(resolved is VexFunctionDef)
        val funcDef = resolved as VexFunctionDef
        val paramType =
            funcDef.parameterListDef?.parameterDefList?.firstOrNull()?.let {
                VexTypeExtractor.extractType(it)
            }
        assertEquals(VexType.FloatType, paramType)
    }

    fun testResolveFunctionReturnsNullWhenNoMatch() {
        val code =
            """
            void testFunc(int a, int b) {}
            void main() {}
            """
                .trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        // Arity mismatch
        val resolvedMismatch =
            VexFunctionResolver.resolveFunction(
                element = file,
                functionName = "testFunc",
                argTypes = listOf(VexType.IntType),
            )
        assertNull(resolvedMismatch)

        // Non-existent function
        val resolvedNonExistent =
            VexFunctionResolver.resolveFunction(
                element = file,
                functionName = "nonExistent",
                arity = 0,
            )
        assertNull(resolvedNonExistent)
    }

    fun testIsKnownFunction() {
        val code =
            """
            void myCustomFunc() {}
            void main() {}
            """
                .trimIndent()
        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        assertTrue(VexFunctionResolver.isKnownFunction("myCustomFunc", file))
        assertTrue(VexFunctionResolver.isKnownFunction("sin", file)) // standard API
        assertFalse(VexFunctionResolver.isKnownFunction("completely_fake_function_xyz", file))
    }
}
