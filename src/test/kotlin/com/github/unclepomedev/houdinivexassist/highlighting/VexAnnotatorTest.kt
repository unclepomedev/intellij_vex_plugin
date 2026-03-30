package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType

class VexAnnotatorTest : VexTestBase() {

    fun testValidFunctionIsNotHighlightedAsError() {
        myFixture.configureByText(VexFileType, "float d = distance(p1, p2);")
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testUnknownFunctionIsHighlightedAsError() {
        myFixture.configureByText(
            VexFileType,
            "float d = <error descr=\"Unknown VEX function: 'hogehoge'\">hogehoge</error>(p1, p2);"
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testLocalFunctionIsNotHighlightedAsError() {
        myFixture.configureByText(
            VexFileType,
            """
            float myLocalFunc() {
                return 1.0;
            }
            
            void main() {
                float d = myLocalFunc();
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableRedeclarationInSameScope() {
        myFixture.configureByText(
            VexFileType,
            """
            int myVar = 1;
            float <error descr="Variable 'myVar' is already defined in this scope">myVar</error> = 2.0;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableRedeclarationWithParameter() {
        myFixture.configureByText(
            VexFileType,
            """
            void myFunc(int myParam) {
                float <error descr="Variable 'myParam' is already defined as a parameter">myParam</error> = 1.0;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testValidVariableShadowingInDifferentScope() {
        myFixture.configureByText(
            VexFileType,
            """
            int myVar = 1;
            if (myVar > 0) {
                int myVar = 2;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testValidVariableInDifferentStructs() {
        myFixture.configureByText(
            VexFileType,
            """
            struct A { int val; }
            struct B { int val; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testUnresolvedVariableIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            int a = 1;
            int b = a + <error descr="Unresolved variable: 'c'">c</error>;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testResolvedVariableIsNotHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            int outerVar = 1;
            void myFunc(int paramVar) {
                if (outerVar > 0) {
                    int innerVar = outerVar + paramVar;
                    @P.x += innerVar;
                }
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableConflictsWithStandardFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            float distance = 1.0;
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionCallShadowedByVariable() {
        myFixture.configureByText(
            VexFileType,
            """
            float distance = 1.0;
            <error descr="Variable 'distance' cannot be called as a function">distance</error>(@P, @P);
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariableConflictsWithLocalFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            void myLocalFunc() {}
            
            void main() {
                int <error descr="Variable name 'myLocalFunc' conflicts with a local function">myLocalFunc</error> = 1;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionConflictsWithStandardFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            void <error descr="Function name 'normalize' conflicts with a standard VEX function">normalize</error>(vector v) {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionConflictsWithStruct() {
        myFixture.configureByText(
            VexFileType,
            """
            struct MyData { int a; }
            
            void <error descr="Function name 'MyData' conflicts with a struct definition">MyData</error>() {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionExactOverloadConflict() {
        myFixture.configureByText(
            VexFileType,
            """
            void myFunc(int a) {}
            
            void myFunc(int a, float b) {}
            
            void <error descr="Function 'myFunc' with 1 parameters is already defined">myFunc</error>(int a) {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStructConflictsWithStruct() {
        myFixture.configureByText(
            VexFileType,
            """
            struct MyData { int a; }
            struct <error descr="Struct 'MyData' is already defined">MyData</error> { float b; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStructConflictsWithStandardFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            struct <error descr="Struct name 'length' conflicts with a standard VEX function">length</error> { int val; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStructConflictsWithLocalFunction() {
        myFixture.configureByText(
            VexFileType,
            """
            void myProc() {}
            
            struct <error descr="Struct name 'myProc' conflicts with a local function">myProc</error> { int val; }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testForwardDeclaredFunctionCall() {
        myFixture.configureByText(
            VexFileType,
            """
        float d = myLaterFunc();
        
        float myLaterFunc() { return 0.0; }
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testUnusedVariableIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                int <weak_warning descr="Unused variable 'myVar'">myVar</weak_warning> = 1;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testUsedVariableIsNotHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                int myVar = 1;
                @P.x += myVar;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testUnusedParameterIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            void myFunc(int <weak_warning descr="Unused parameter 'unusedParam'">unusedParam</weak_warning>, int usedParam) {
                int <weak_warning descr="Unused variable 'a'">a</weak_warning> = usedParam;
            }
            
            void main() {
                myFunc(1, 2);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testUnusedFunctionIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            void <weak_warning descr="Unused function 'myHelper'">myHelper</weak_warning>() {
            }
            
            void main() {
                // main is not marked as unused
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testUsedFunctionIsNotHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            void myHelper() {
            }
            
            void main() {
                myHelper();
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testUnusedStructFieldIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            struct Engine {
                int <weak_warning descr="Unused field 'power'">power</weak_warning>;
                float <weak_warning descr="Unused field 'torque'">torque</weak_warning>;
            }

            void main() {
                Engine e;
            }
            """.trimIndent()
        )
        // e is also unused
        myFixture.checkHighlighting(true, false, true, true)
    }

    fun testUsedStructFieldIsNotHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            struct Engine {
                int power;
                float torque;
            }

            void main() {
                Engine e;
                e.power = 100;
                float <weak_warning descr="Unused variable 't'">t</weak_warning> = e.torque;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testMissingSemicolonIsHighlighted() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                vector pos = {0, 0, 0};
                // no semicolon
                <error descr="Missing ';'">pos.x</error>
                
                int a = 1;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStructFieldShadowingCollisionWithAandB() {
        myFixture.configureByText(
            VexFileType,
            """
            struct A { int power; }
            struct B { int <weak_warning descr="Unused field 'power'">power</weak_warning>; }

            void main() {
                A objA;
                objA.power = 100;
                
                B objB;
            }
            """.trimIndent()
        )
        // objB is also unused
        myFixture.checkHighlighting(true, false, true, true)
    }

    fun testTypeCheckInDeclaration() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                int a = 1;         // OK
                vector v = 1.0;    // OK (Implicit cast)
                
                int b = <error descr="Incompatible types: cannot assign 'string' to 'int'">"hello"</error>;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testTypeCheckInAssignment() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                string s = "test";
                vector v = {1, 2, 3};
                
                s = <error descr="Incompatible types: cannot assign 'vector' to 'string'">v</error>;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testTypeCheckFunctionReturnTypeAssignment() {
        myFixture.configureByText(
            VexFileType,
            """
            void myVoidFunc() {}
            vector myVectorFunc() { return {1,2,3}; }

            void main() {
                float f = <error descr="Incompatible types: cannot assign 'void' to 'float'">myVoidFunc()</error>;
                string s = <error descr="Incompatible types: cannot assign 'vector' to 'string'">myVectorFunc()</error>;
                vector v = myVectorFunc();
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testCompoundAssignmentTypeCheck() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                string s = "test";
                s += 1; // OK: string + int = string
                
                vector v = {1,2,3};
                matrix m = 1;
                v *= m; // OK: vector * matrix = vector
                
                int i = 1;
                <error descr="Incompatible types: cannot assign result of type 'string' to 'int'">i += "text"</error>;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStrictNumericAssignmentCheck() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                vector2 v2;
                vector4 v4;
                
                v2 = <error descr="Incompatible types: cannot assign 'vector4' to 'vector2'">v4</error>;
                
                matrix m;
                vector v;
                m = <error descr="Incompatible types: cannot assign 'vector' to 'matrix'">v</error>;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testArrayAndStructAssignmentCheck() {
        myFixture.configureByText(
            VexFileType,
            """
            struct A { int val; }
            struct B { int val; }
            
            void main() {
                int int_arr[];
                float float_arr[];
                
                int_arr = <error descr="Incompatible types: cannot assign 'float[]' to 'int[]'">float_arr</error>;
                
                A a_obj;
                B b_obj;
                
                a_obj = <error descr="Incompatible types: cannot assign 'struct B' to 'struct A'">b_obj</error>;
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionArgumentTypeCheck() {
        myFixture.configureByText(
            VexFileType,
            """
            void myFunc(int a, vector v) {}

            void main() {
                myFunc(1, {0,0,0}); // OK
                myFunc(1, 2.0);     // OK implicit cast

                myFunc(<error descr="Type mismatch in argument 1: expected 'int', got 'string'">"text"</error>, {0,0,0});
                
                float d = distance({0,0,0}, <error descr="Type mismatch in argument 2: expected 'vector', got 'string'">"string"</error>);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testOverloadResolutionChoosesBestError() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                // 'set' has overloads like set(float, float, float) and set(vector, vector, vector).
                // passing (float, string, float) gives exact matches for the float overload.
                // The exact-match tie-breaker selects the float overload.
                vector v = set(1.0, <error descr="Type mismatch in argument 2: expected 'float', got 'string'">"string"</error>, 3.0);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testLocalOverloadResolutionByTypeSignature() {
        myFixture.configureByText(
            VexFileType,
            """
            function void process(int a, int b) {}
            function void process(string a, string b) {}

            void main() {
                process(1, 2);           // OK: matches (int, int)
                process("a", "b");       // OK: matches (string, string)
                process(1, <error descr="Type mismatch in argument 2: expected 'int', got 'string'">"text"</error>);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testLocalOverloadResolutionByTypeSignatureDifferentArgNum() {
        myFixture.configureByText(
            VexFileType,
            """
            function void process(int a) {}
            function void process(int a, int b) {}
            function void process(string a, string b, string c) {}

            void main() {
                process(1);              // OK: matches (int)
                process(1, 2);           // OK: matches (int, int)
                process("a", "b", "c");  // OK: matches (string, string, string)
                process(1, <error descr="Type mismatch in argument 2: expected 'int', got 'string'">"text"</error>);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testApiArrayParameterSupport() {
        myFixture.configureByText(
            VexFileType,
            """
            void myProcessIntArray(int arr[], int val) {}
            
            void main() {
                int valid_arr[] = {1, 2, 3};
                float invalid_arr[] = {1.0, 2.0, 3.0};
                
                myProcessIntArray(valid_arr, 4); // OK: (int[], int)
                
                myProcessIntArray(<error descr="Type mismatch in argument 1: expected 'int[]', got 'float[]'">invalid_arr</error>, 4);
                
                myProcessIntArray(valid_arr, <error descr="Type mismatch in argument 2: expected 'int', got 'string'">"text"</error>);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionExactOverloadConflictWithDifferentParameterNames() {
        myFixture.configureByText(
            VexFileType,
            """
            // First definition
            void myFunc(int a, float b) {}
            
            // Second definition: different parameter names, but same type signature
            void <error descr="Function 'myFunc' with 2 parameters is already defined">myFunc</error>(int x, float y) {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFunctionExactOverloadConflictWithArraySyntax() {
        myFixture.configureByText(
            VexFileType,
            """
            // First definition
            void processArray(int arr[]) {}
            
            // Second definition: identical signature, should trigger conflict error
            void <error descr="Function 'processArray' with 1 parameters is already defined">processArray</error>(int arr[]) {}
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testUnresolvedFunctionWithWrongArityIsNotFallbackToAnotherOverload() {
        myFixture.configureByText(
            VexFileType,
            """
            // Define overloads with 1 and 2 arguments
            void myProc(int a) {}
            void myProc(int a, float b) {}
            
            void main() {
                // Calling with 3 arguments. 
                // It should NOT fall back to myProc(int, float).
                // It should correctly report that no matching overload exists.
                <error descr="No matching overload for function 'myProc' with 3 arguments">myProc</error>(1, 2.0, 3.0);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testReturnStatementTypeCheck() {
        myFixture.configureByText(
            VexFileType,
            """
            void myVoidFunc() {
                if (1) return; // OK
                return <error descr="Cannot return a value from a function returning 'void'">1</error>;
            }

            int myIntFunc() {
                <error descr="Missing return value: expected 'int'">return;</error>
            }

            float myFloatFunc() {
                return 1; // OK: int implicitly casts to float
            }

            string myStringFunc() {
                return <error descr="Incompatible return type: expected 'string', got 'vector'">{1, 2, 3}</error>;
            }

            vector myVectorFunc() {
                return 1.0; // OK: float implicitly casts to vector
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testFileNamedFunctionIsNotHighlightedAsUnused() {
        myFixture.configureByText(
            "my_wrangle.vex", """
            void my_wrangle() {
                int a = 1;
                @P.x += a;
            }
        """.trimIndent()
        )

        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testDifferentNamedFunctionIsHighlightedAsUnused() {
        myFixture.configureByText(
            "my_wrangle.vex", """
            void <weak_warning descr="Unused function 'another_func'">another_func</weak_warning>() {
                int <weak_warning descr="Unused variable 'a'">a</weak_warning> = 1;
            }
        """.trimIndent()
        )

        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testSanitizedFileNamedFunctionIsNotHighlighted() {
        myFixture.configureByText(
            "my-awesome-wrangle.vex", """
            void my_awesome_wrangle() {
                int a = 1;
                @P.x += a;
            }
        """.trimIndent()
        )

        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testSameNameVariableAndFunctionCallOnSameLine() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                float dot = dot({1,0,0}, {0,1,0});
                float len = length({1,2,3});
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testNestedFunctionCallInInitializer() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                float dot = abs(dot({1,0,0}, {0,1,0}));
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testSelfInitializationIsHighlightedAsError() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                int a = <error descr="Variable 'a' is used in its own initialization">a</error>;
                int b = <error descr="Variable 'b' is used in its own initialization">b</error> + 1;
                vector v = {<error descr="Variable 'v' is used in its own initialization">v</error>.x, 0, 0};
                
                int c = 1;
                int d = c + 1;
                
                if (1) {
                    int a = 10;
                }
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testVariadicFunctionSupport() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                printf("%d\n", 3);
                printf("%d %f %s\n", 1, 2.0, "test");
                printf("no args");
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testIncludeHighlighting() {
        myFixture.addFileToProject(
            "my_lib.vfl",
            """
            void my_lib_func() {}
            int my_lib_var = 123;
            """.trimIndent()
        )
        myFixture.configureByText(
            VexFileType,
            """
            #include "my_lib.vfl"
            void main() {
                my_lib_func();
                int a = my_lib_var;
            }
            """.trimIndent()
        )
        // Everything should be resolved, no errors
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testIncludeUnusedWarning() {
        val libFile = myFixture.addFileToProject(
            "my_lib.vfl",
            """
            void my_lib_func() {}
            int my_lib_var = 123;
            """.trimIndent()
        )
        myFixture.configureByText(
            VexFileType,
            """
            #include "my_lib.vfl"
            void main() {
                my_lib_func();
                int a = my_lib_var;
                a = 2; // to mark 'a' as used
            }
            """.trimIndent()
        )
        // No unused warnings for my_lib_func and my_lib_var because they are used in main
        myFixture.checkHighlighting(true, false, true, false)

        // Verify the included file also has no unused warnings
        myFixture.openFileInEditor(libFile.virtualFile)
        myFixture.checkHighlighting(true, false, true, false)
    }

    fun testTypeCastCallInFunctionArg() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                int a = 10;
                int b = floor(@ptnum / float(a));
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testAmbiguousAttributeInBinaryExprNoFalseError() {
        myFixture.configureByText(
            VexFileType,
            """
            void main() {
                int a = @I * 2;
                float b = @I + 1.0;
                vector v = @I * {1, 2, 3};
            }
            """.trimIndent()
        )
        // @I has conflicting types (vector and int) in the builtin data,
        // so it resolves to UnknownType. No error should be reported
        // because UnknownType is always assignable.
        myFixture.checkHighlighting(false, false, false, false)
    }
}
