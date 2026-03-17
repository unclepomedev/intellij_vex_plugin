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
            float <error descr="Variable name 'distance' conflicts with a standard VEX function">distance</error> = 1.0;
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
                // Both score 2/3 matches (since float casts to vector). The checker picks one correctly.
                vector v = set(1.0, <error descr="Type mismatch in argument 2: expected 'vector', got 'string'">"string"</error>, 3.0);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    fun testStrictParameterTypeMismatch() {
        myFixture.configureByText(
            VexFileType,
            """
            struct IntData { int val; }
            struct FloatData { float val; }
            
            void myProcessData(IntData data, int val) {}
            
            void main() {
                IntData valid_data;
                FloatData invalid_data;
                
                myProcessData(valid_data, 4); // OK
                
                myProcessData(<error descr="Type mismatch in argument 1: expected 'struct IntData', got 'struct FloatData'">invalid_data</error>, 4);
                
                myProcessData(valid_data, <error descr="Type mismatch in argument 2: expected 'int', got 'string'">"text"</error>);
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting(false, false, false, false)
    }

    // TODO: after fix parser bug concerning arr[], add tests
}
