package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.psi.util.PsiTreeUtil

class VexTypeExtractorTest : VexTestBase() {

    fun testExtractType() {
        val code = """
            string myFunc(float p1, matrix m) {
                int a = 1;
                float b[];
                vector v = {1.0, 2.0, 3.0};
                return "test";
            }
            
            void main() {}
        """.trimIndent()

        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val functionDefs = PsiTreeUtil.findChildrenOfType(file, VexFunctionDef::class.java).toList()
        val paramDefs = PsiTreeUtil.findChildrenOfType(file, VexParameterDef::class.java).toList()
        val declItems = PsiTreeUtil.findChildrenOfType(file, VexDeclarationItem::class.java).toList()

        assertEquals(2, functionDefs.size)
        assertEquals(VexType.StringType, VexTypeExtractor.extractType(functionDefs[0])) // string myFunc
        assertEquals(VexType.VoidType, VexTypeExtractor.extractType(functionDefs[1]))   // void main

        assertEquals(2, paramDefs.size)
        assertEquals(VexType.FloatType, VexTypeExtractor.extractType(paramDefs[0]))  // float p1
        assertEquals(VexType.MatrixType, VexTypeExtractor.extractType(paramDefs[1])) // matrix m

        assertEquals(3, declItems.size)
        assertEquals(VexType.IntType, VexTypeExtractor.extractType(declItems[0])) // int a
        assertEquals(VexType.ArrayType(VexType.FloatType), VexTypeExtractor.extractType(declItems[1])) // float b[]
        assertEquals(VexType.VectorType, VexTypeExtractor.extractType(declItems[2])) // vector v
    }

    fun testExtractUnknownType() {
        val code = """
            int a = 1;
        """.trimIndent()

        myFixture.configureByText(VexFileType, code)
        val file = myFixture.file as VexFile

        val primaryExpr = PsiTreeUtil.findChildOfType(file, VexPrimaryExpr::class.java)
        assertNotNull(primaryExpr)

        assertEquals(VexType.UnknownType, VexTypeExtractor.extractType(primaryExpr!!))
    }
}
