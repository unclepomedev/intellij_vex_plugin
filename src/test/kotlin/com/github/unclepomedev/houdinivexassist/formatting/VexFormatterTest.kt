package com.github.unclepomedev.houdinivexassist.formatting

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.application.options.CodeStyle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager

class VexFormatterTest : VexTestBase() {

    override fun setUp() {
        super.setUp()
        val settings = CodeStyle.getSettings(project).getCommonSettings(VexLanguage.INSTANCE)
        settings.indentOptions?.apply {
            INDENT_SIZE = 4
            CONTINUATION_INDENT_SIZE = 4
            TAB_SIZE = 4
            USE_TAB_CHARACTER = false
        }
    }

    private fun reformatAndAssert(before: String, after: String) {
        myFixture.configureByText(VexFileType, before)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        myFixture.checkResult(after)
    }

    private fun reformatTextAndAssert(before: String, after: String) {
        myFixture.configureByText(VexFileType, before)
        WriteCommandAction.runWriteCommandAction(project) {
            val file = myFixture.file
            CodeStyleManager.getInstance(project).reformatText(file, 0, file.textLength)
        }
        myFixture.checkResult(after)
    }

    fun testBasicFormatting() {
        val before = """
            int   myFunc ( int a,int b ){
                int c=a+b ;
                return c;
            }
        """.trimIndent()

        val after = """
            int myFunc(int a, int b) {
                int c = a + b;
                return c;
            }
        """.trimIndent()

        reformatAndAssert(before, after)
    }

    fun testComprehensiveFormatting() {
        val before = """
            struct   MyData{
            int  val;
            }
            
            int   myFunc ( int a,int b ){
                int c=a+b ;
                    int d = -5;
                    
                    if (c==d){
                    c+=1;
                    }
                
                return c;
            }
            
            void test ( ){
            myFunc ( 1 ,-2) ;
            }
        """.trimIndent()

        val after = """
            struct MyData {
                int val;
            }
            
            int myFunc(int a, int b) {
                int c = a + b;
                int d = -5;
            
                if (c == d) {
                    c += 1;
                }
            
                return c;
            }
            
            void test() {
                myFunc(1, -2);
            }
        """.trimIndent()

        reformatAndAssert(before, after)
    }

    fun testTrailingNewlineAdded() {
        reformatTextAndAssert("int x = 1;", "int x = 1;\n")
    }

    fun testTrailingNewlineNotDuplicated() {
        reformatTextAndAssert("int x = 1;\n", "int x = 1;\n")
    }

    fun testEmptyFileNewlineNotAdded() {
        reformatTextAndAssert("", "")
    }

    fun testAdvancedOperatorsAndMultilineLists() {
        val before = """
            int myFunc (
            int a,
            int b
            ) {
                int x=a<<2 ;
                int y=b>>1;
                int z = a|b&x^y ;
                return x>y?x :y;
            }

            void test ( ){
                myFunc (
                1 ,
                2
                ) ;
            }
        """.trimIndent()

        val after = """
            int myFunc(
                int a,
                int b
            ) {
                int x = a << 2;
                int y = b >> 1;
                int z = a | b & x ^ y;
                return x > y ? x : y;
            }

            void test() {
                myFunc(
                    1,
                    2
                );
            }
        """.trimIndent()

        reformatAndAssert(before, after)
    }

    fun testMacroWithLineContinuation() {
        val before = """
            #define FOO \
            1
            #define BAR(a,b) \
              ((a) + \
               (b))
            int x = 1;
        """.trimIndent()

        // No complicated formatting
        val after = """
            #define FOO \
            1
            #define BAR(a, b) \
              ((a) + \
               (b))
            int x = 1;
        """.trimIndent()

        reformatAndAssert(before, after)
    }
}
