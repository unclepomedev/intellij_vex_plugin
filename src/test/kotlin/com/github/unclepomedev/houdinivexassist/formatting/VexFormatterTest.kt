package com.github.unclepomedev.houdinivexassist.formatting

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager

class VexFormatterTest : VexTestBase() {

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

        myFixture.configureByText(VexFileType, before)

        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }

        myFixture.checkResult(after)
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

        myFixture.configureByText(VexFileType, before)

        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }

        myFixture.checkResult(after)
    }
}