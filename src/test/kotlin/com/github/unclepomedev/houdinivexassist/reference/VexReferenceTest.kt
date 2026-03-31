package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.psi.VexDeclarationItem
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionDef
import com.github.unclepomedev.houdinivexassist.psi.VexMacroDef
import com.github.unclepomedev.houdinivexassist.psi.VexParameterDef
import com.github.unclepomedev.houdinivexassist.psi.VexStructDef
import java.nio.file.Files

class VexReferenceTest : VexTestBase() {

    fun testVariableReference() {
        myFixture.configureByText(
            VexFileType, """
            function void test() {
                int myVar = 1;
                m<caret>yVar = 2;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Reference should be resolved", resolved)
        assertTrue("Resolved element should be a declaration", resolved is VexDeclarationItem)
    }

    fun testParameterReference() {
        myFixture.configureByText(
            VexFileType, """
            function void test(int param1) {
                p<caret>aram1 = 2;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Reference should be resolved", resolved)
        assertTrue("Resolved element should be a parameter definition", resolved is VexParameterDef)
    }

    fun testFunctionReference() {
        myFixture.configureByText(
            VexFileType, """
            void myTargetFunction(int a) {
            }

            void main() {
                myTarg<caret>etFunction(1);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)
        assertEquals("myTargetFunction", (resolved as VexFunctionDef).identifier.text)
    }

    fun testFunctionOverloadReference() {
        myFixture.configureByText(
            VexFileType, """
            void myFunc(int a) {}
            
            void myFunc(int a, float b) {} 

            void main() {
                myF<caret>unc(1, 2.0);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)

        val funcDef = resolved as VexFunctionDef
        assertEquals("myFunc", funcDef.identifier.text)

        val paramCount = funcDef.parameterListDef?.parameterDefList?.size ?: 0
        assertEquals("Should resolve to the overload with 2 parameters", 2, paramCount)
    }

    fun testFunctionOverloadReferenceByTypeSignature() {
        myFixture.configureByText(
            VexFileType, """
            void process(int a) {}      // candidate 1 (Int)
            void process(string a) {}   // candidate 2 (String)
            void process(float a) {}    // candidate 3 (Float)

            void main() {
                // This should jump to the string overload, not the first one defined.
                proce<caret>ss("hello");
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)

        val funcDef = resolved as VexFunctionDef
        assertEquals("process", funcDef.identifier.text)

        // Ensure we jumped to the correct overload by checking the parameter type
        val paramType = funcDef.parameterListDef?.parameterDefList?.firstOrNull()?.typeRef?.text
        assertEquals("Should resolve to the overload with string parameter", "string", paramType)
    }

    fun testFunctionOverloadReferenceWithImplicitCast() {
        myFixture.configureByText(
            VexFileType, """
            void set(int a, int b) {}       // candidate 1
            void set(vector a, vector b) {} // candidate 2

            void main() {
                // This should jump to the vector overload due to implicit casting from float
                s<caret>et({1,2,3}, 2.0);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function reference should be resolved", resolved)
        assertTrue("Resolved element should be a function definition", resolved is VexFunctionDef)

        val funcDef = resolved as VexFunctionDef
        assertEquals("set", funcDef.identifier.text)

        // Ensure we jumped to the correct overload by checking the parameter type
        val paramType = funcDef.parameterListDef?.parameterDefList?.firstOrNull()?.typeRef?.text
        assertEquals("Should resolve to the overload with vector parameter via implicit cast", "vector", paramType)
    }

    fun testVariableRename() {
        myFixture.configureByText(
            VexFileType, """
            function void test() {
                int oldVar = 1;
                <caret>oldVar = 2;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNotNull("Resolve failed", ref.resolve())

        myFixture.renameElementAtCaret("newVarName")

        myFixture.checkResult(
            """
            function void test() {
                int newVarName = 1;
                newVarName = 2;
            }
        """.trimIndent()
        )
    }

    fun testFunctionParameterRename() {
        myFixture.configureByText(
            VexFileType, """
            void myFunc(int p) {
                <caret>p = 10;
            }
        """.trimIndent()
        )

        myFixture.renameElementAtCaret("paramNew")

        myFixture.checkResult(
            """
            void myFunc(int paramNew) {
                paramNew = 10;
            }
        """.trimIndent()
        )
    }

    fun testFunctionRename() {
        myFixture.configureByText(
            VexFileType, """
            void oldName() {
            }

            void main() {
                old<caret>Name();
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNotNull("Resolve failed", ref.resolve())

        myFixture.renameElementAtCaret("newName")

        myFixture.checkResult(
            """
            void newName() {
            }

            void main() {
                newName();
            }
        """.trimIndent()
        )
    }

    fun testFunctionDeclarationRename() {
        myFixture.configureByText(
            VexFileType, """
            void old<caret>Name() {
            }

            void main() {
                oldName();
            }
        """.trimIndent()
        )

        myFixture.renameElementAtCaret("newName")

        myFixture.checkResult(
            """
            void newName() {
            }

            void main() {
                newName();
            }
        """.trimIndent()
        )
    }

    fun testIncludeReference() {
        myFixture.addFileToProject("my_math.vfl", "int add(int a, int b) { return a + b; }")
        myFixture.configureByText(
            VexFileType, """
            #include "my_<caret>math.vfl"
            void main() {
                add(1, 2);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Include reference should be resolved", resolved)
        assertTrue(
            "Resolved element should be a VexFile",
            resolved is com.github.unclepomedev.houdinivexassist.psi.VexFile
        )
        assertEquals("my_math.vfl", (resolved as com.github.unclepomedev.houdinivexassist.psi.VexFile).name)
    }

    fun testIncludeRename() {
        myFixture.addFileToProject("my_math.vfl", "int add(int a, int b) { return a + b; }")
        myFixture.configureByText(
            VexFileType, """
            #include "my_<caret>math.vfl"
            void main() {
                add(1, 2);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNotNull("Resolve failed", ref.resolve())

        myFixture.renameElementAtCaret("new_math.vfl")

        myFixture.checkResult(
            """
            #include "new_math.vfl"
            void main() {
                add(1, 2);
            }
        """.trimIndent()
        )
    }

    fun testIncludeHeaderFileReference() {
        myFixture.addFileToProject("my_math.h", "int add_header(int a, int b) { return a + b; }")
        myFixture.configureByText(
            VexFileType, """
            #include "my_<caret>math.h"
            void main() {
                add_header(1, 2);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Include reference should be resolved", resolved)
        assertTrue(
            "Resolved element should be a PsiFile",
            resolved is com.intellij.psi.PsiFile
        )
        assertEquals("my_math.h", (resolved as com.intellij.psi.PsiFile).name)

        // Ensure that function reference from the .h file resolves correctly
        myFixture.configureByText(
            VexFileType, """
            #include "my_math.h"
            void main() {
                add_head<caret>er(1, 2);
            }
        """.trimIndent()
        )

        val funcRef = myFixture.getReferenceAtCaretPositionWithAssertion()
        val funcResolved = funcRef.resolve()
        assertNotNull("Function reference from .h should be resolved", funcResolved)
        assertTrue(funcResolved is VexFunctionDef)
        assertEquals(
            "add_header",
            (funcResolved as VexFunctionDef).identifier.text
        )
    }

    fun testIncludeRenameWithDirectoryPath() {
        myFixture.addFileToProject("subdir/my_math.vfl", "int add(int a, int b) { return a + b; }")
        myFixture.configureByText(
            VexFileType, """
            #include "subdir/my_<caret>math.vfl"
            void main() {
                add(1, 2);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNotNull("Resolve failed", ref.resolve())

        myFixture.renameElementAtCaret("new_math.vfl")

        myFixture.checkResult(
            """
            #include "subdir/new_math.vfl"
            void main() {
                add(1, 2);
            }
        """.trimIndent()
        )
    }

    fun testIncludeHeaderFileRelativeResolution() {
        myFixture.addFileToProject("lib/core.h", "int core_func() { return 1; }")
        myFixture.addFileToProject("lib/wrapper.h", "#include \"core.h\"\nint wrapper_func() { return core_func(); }")

        myFixture.configureByText(
            VexFileType, """
            #include "lib/wrapper.h"
            void main() {
                wrap<caret>per_func();
            }
        """.trimIndent()
        )

        val funcRef = myFixture.getReferenceAtCaretPositionWithAssertion()
        val funcResolved = funcRef.resolve()

        assertNotNull("Function reference from nested .h should be resolved", funcResolved)
        assertTrue(funcResolved is VexFunctionDef)
        assertEquals("wrapper_func", (funcResolved as VexFunctionDef).identifier.text)

        myFixture.configureByText(
            VexFileType, """
            #include "lib/wrapper.h"
            void main() {
                cor<caret>e_func();
            }
        """.trimIndent()
        )

        val nestedFuncRef = myFixture.getReferenceAtCaretPositionWithAssertion()
        val nestedFuncResolved = nestedFuncRef.resolve()
        assertNotNull("Function reference from deeply nested .h should be resolved", nestedFuncResolved)
        assertTrue(nestedFuncResolved is VexFunctionDef)
        assertEquals("core_func", (nestedFuncResolved as VexFunctionDef).identifier.text)
    }

    fun testIncludeSystemHeaderFileReference() {
        // test my_sys_lib.h included via <my_sys_lib.h>
        val headerCode = """
            void my_sys_lib_func() {
            }
        """.trimIndent()
        myFixture.addFileToProject("my_sys_lib.h", headerCode)

        val mainCode = """
            #include <my_sys_lib.h>
            
            void main() {
                my_sys_lib_f<caret>unc();
            }
        """.trimIndent()

        myFixture.configureByText("main.vfl", mainCode)

        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("Function reference should be resolved", reference)

        val resolved = reference?.resolve()
        assertNotNull("Reference should resolve to a valid element", resolved)
        assertTrue("Resolved element should be a VexFunctionDef", resolved is VexFunctionDef)
        assertEquals("my_sys_lib_func", (resolved as VexFunctionDef).name)

        // ensure no parse errors
        myFixture.checkHighlighting(false, false, false)
    }

    fun testIncludeRenameSystemHeaderFile() {
        myFixture.addFileToProject("my_sys_lib.h", "int add(int a, int b) { return a + b; }")
        myFixture.configureByText(
            VexFileType, """
            #include <my_sys_<caret>lib.h>
            void main() {
                add(1, 2);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        assertNotNull("Resolve failed", ref.resolve())

        myFixture.renameElementAtCaret("new_sys_lib.h")

        myFixture.checkResult(
            """
            #include <new_sys_lib.h>
            void main() {
                add(1, 2);
            }
        """.trimIndent()
        )
    }

    fun testStructReference() {
        myFixture.configureByText(
            VexFileType, """
            struct MyStruct { int a; }
            void main() {
                MySt<caret>ruct s;
            }
        """.trimIndent()
        )
        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()
        assertNotNull("Struct reference should be resolved", resolved)
        assertTrue(resolved is VexStructDef)
        assertEquals("MyStruct", (resolved as VexStructDef).identifier?.text)
    }

    fun testStructRename() {
        myFixture.configureByText(
            VexFileType, """
            struct MyStruct { int a; }
            void main() {
                MySt<caret>ruct s;
            }
        """.trimIndent()
        )
        myFixture.renameElementAtCaret("NewStruct")
        myFixture.checkResult(
            """
            struct NewStruct { int a; }
            void main() {
                NewStruct s;
            }
        """.trimIndent()
        )
    }

    fun testStructMemberReference() {
        myFixture.configureByText(
            VexFileType, """
            struct MyStruct { int my_field; }
            void main() {
                MyStruct s;
                s.my_fie<caret>ld = 1;
            }
        """.trimIndent()
        )
        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()
        assertNotNull("Struct member reference should be resolved", resolved)
        assertTrue(resolved is VexDeclarationItem)
        assertEquals("my_field", (resolved as VexDeclarationItem).identifier.text)
    }

    fun testStructMemberRename() {
        myFixture.configureByText(
            VexFileType, """
            struct MyStruct { int my_field; }
            void main() {
                MyStruct s;
                s.my_fie<caret>ld = 1;
            }
        """.trimIndent()
        )
        myFixture.renameElementAtCaret("new_field")
        myFixture.checkResult(
            """
            struct MyStruct { int new_field; }
            void main() {
                MyStruct s;
                s.new_field = 1;
            }
        """.trimIndent()
        )
    }

    fun testMacroDefReference() {
        myFixture.configureByText(
            VexFileType, """
            #define MY_VAL 10
            void main() {
                int x = MY_<caret>VAL;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Macro reference should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("MY_VAL", (resolved as VexMacroDef).identifier?.text)
    }

    fun testMacroDefFromIncludedFile() {
        myFixture.addFileToProject("constants.h", "#define INCLUDED_CONST 42")
        myFixture.configureByText(
            VexFileType, """
            #include "constants.h"
            void main() {
                int x = INCLUDED_<caret>CONST;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Macro reference from included file should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("INCLUDED_CONST", (resolved as VexMacroDef).identifier?.text)
    }

    fun testIncludePathWithSpecialCharacters() {
        val tempDir = Files.createTempDirectory("vex_include_test")
        val libFile = tempDir.resolve("lib.vfl").toFile()
        libFile.writeText("void my_lib_func() {}")

        val vfsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir.toFile())
        assertNotNull("Temp directory should be found by VFS", vfsDir)

        val settings = com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(com.github.unclepomedev.houdinivexassist.settings.VexSettingsState::class.java)
        val oldPath = settings.includePath
        try {
            // Append ; and & to the path
            settings.includePath = "${tempDir.toAbsolutePath()};&"

            myFixture.configureByText(
                VexFileType, """
                #include "l<caret>ib.vfl"
                void main() {
                    my_lib_func();
                }
            """.trimIndent()
            )

            val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
            val resolved = ref.resolve()
            assertNotNull("Include reference should be resolved using complex include paths", resolved)
            assertTrue("Resolved element should be a file", resolved is com.intellij.psi.PsiFile)
            assertEquals("lib.vfl", (resolved as com.intellij.psi.PsiFile).name)
        } finally {
            settings.includePath = oldPath
            Files.deleteIfExists(libFile.toPath())
            Files.deleteIfExists(tempDir.toFile().toPath())
        }
    }

    fun testMacroRedefinitionAcrossIncludes() {
        myFixture.addFileToProject("def_first.h", "#define OVERRIDE_ME 1")
        myFixture.addFileToProject("def_second.h", "#define OVERRIDE_ME 2")

        myFixture.configureByText(
            VexFileType, """
            #include "def_first.h"
            #include "def_second.h"
            
            void main() {
                int x = OVERRIDE_<caret>ME;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Overridden macro reference should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("def_second.h", resolved?.containingFile?.name)
    }

    fun testMacroLocalOverridesInclude() {
        myFixture.addFileToProject("base_const.h", "#define CONFIG_VAL 100")

        val mainFile = myFixture.configureByText(
            VexFileType, """
            #include "base_const.h"
            
            #define CONFIG_VAL 999
            
            void main() {
                int x = CONFIG_<caret>VAL;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Local override macro should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals(mainFile.name, resolved?.containingFile?.name)
    }

    fun testMacroCircularIncludeProtection() {
        myFixture.addFileToProject(
            "cycle_a.h", """
            #include "cycle_b.h"
            #define VAL_A 10
        """.trimIndent()
        )

        myFixture.addFileToProject(
            "cycle_b.h", """
            #include "cycle_a.h"
            #define VAL_B 20
        """.trimIndent()
        )

        myFixture.configureByText(
            VexFileType, """
            #include "cycle_a.h"
            
            void main() {
                int a = VAL_<caret>A;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Macro in circular include should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("cycle_a.h", resolved?.containingFile?.name)
    }

    fun testFunctionLikeMacroReference() {
        myFixture.configureByText(
            VexFileType, """
            #define ADD(a, b) ((a) + (b))
            void main() {
                int x = AD<caret>D(1, 2);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function-like macro reference should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("ADD", (resolved as VexMacroDef).identifier?.text)
    }

    fun testConstantMacroWithParenBody() {
        myFixture.configureByText(
            VexFileType, """
            #define FOO (1)
            void main() {
                int x = FO<caret>O;
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Constant macro with paren body should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("FOO", (resolved as VexMacroDef).identifier?.text)
        assertNull("Constant macro should not have parameter list", resolved.macroParameterList)
    }

    fun testFunctionLikeMacroWithoutParameters() {
        myFixture.configureByText(
            VexFileType, """
            #define ZERO() 0
            
            void main() {
                int x = ZE<caret>RO();
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function-like macro without parameters should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("ZERO", (resolved as VexMacroDef).identifier?.text)

        myFixture.checkHighlighting(false, false, false)
    }

    fun testFunctionLikeMacroWithoutSpaceAfterParen() {
        myFixture.configureByText(
            VexFileType, """
            #define MULT(a,b)a*b
            
            void main() {
                int x = MU<caret>LT(2, 3);
            }
        """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
        val resolved = ref.resolve()

        assertNotNull("Function-like macro without space after paren should be resolved", resolved)
        assertTrue("Resolved element should be a VexMacroDef", resolved is VexMacroDef)
        assertEquals("MULT", (resolved as VexMacroDef).identifier?.text)

        myFixture.checkHighlighting(false, false, false)
    }
}
