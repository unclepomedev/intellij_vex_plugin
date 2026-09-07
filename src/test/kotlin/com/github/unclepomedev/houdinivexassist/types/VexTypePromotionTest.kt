package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import org.junit.Assert.*
import org.junit.Test

class VexTypePromotionTest {

    @Test
    fun testIsAssignableSameTypes() {
        assertTrue(VexTypePromotion.isAssignable(VexType.IntType, VexType.IntType))
        assertTrue(VexTypePromotion.isAssignable(VexType.FloatType, VexType.FloatType))
        assertTrue(VexTypePromotion.isAssignable(VexType.VectorType, VexType.VectorType))
        assertTrue(VexTypePromotion.isAssignable(VexType.StringType, VexType.StringType))
        assertTrue(VexTypePromotion.isAssignable(VexType.VoidType, VexType.VoidType))
    }

    @Test
    fun testIsAssignableUnknownType() {
        assertTrue(VexTypePromotion.isAssignable(VexType.UnknownType, VexType.IntType))
        assertTrue(VexTypePromotion.isAssignable(VexType.IntType, VexType.UnknownType))
        assertTrue(VexTypePromotion.isAssignable(VexType.UnknownType, VexType.UnknownType))
    }

    @Test
    fun testIsAssignableArrayTypes() {
        val intArray = VexType.ArrayType(VexType.IntType)
        val floatArray = VexType.ArrayType(VexType.FloatType)
        val intNestedArray = VexType.ArrayType(intArray)

        assertTrue(VexTypePromotion.isAssignable(intArray, intArray))
        assertFalse(VexTypePromotion.isAssignable(intArray, floatArray))
        assertFalse(VexTypePromotion.isAssignable(intArray, intNestedArray))
        assertFalse(VexTypePromotion.isAssignable(intArray, VexType.IntType))
    }

    @Test
    fun testIsAssignableStructTypes() {
        val structA = VexType.StructType("MyStruct")
        val structASame = VexType.StructType("MyStruct")
        val structB = VexType.StructType("OtherStruct")

        assertTrue(VexTypePromotion.isAssignable(structA, structASame))
        assertFalse(VexTypePromotion.isAssignable(structA, structB))
        assertFalse(VexTypePromotion.isAssignable(structA, VexType.IntType))
    }

    @Test
    fun testIsAssignableString() {
        assertFalse(VexTypePromotion.isAssignable(VexType.StringType, VexType.IntType))
        assertFalse(VexTypePromotion.isAssignable(VexType.IntType, VexType.StringType))
        assertFalse(VexTypePromotion.isAssignable(VexType.StringType, VexType.FloatType))
        assertFalse(VexTypePromotion.isAssignable(VexType.FloatType, VexType.StringType))
    }

    @Test
    fun testIsAssignableNumericScalarToScalar() {
        // Int and Float can be assigned to each other implicitly in VEX
        assertTrue(VexTypePromotion.isAssignable(VexType.FloatType, VexType.IntType))
        assertTrue(VexTypePromotion.isAssignable(VexType.IntType, VexType.FloatType))
    }

    @Test
    fun testIsAssignableVectorAndMatrix() {
        // Scalar to Vector is allowed (e.g., v = 1.0)
        assertTrue(VexTypePromotion.isAssignable(VexType.VectorType, VexType.FloatType))
        assertTrue(VexTypePromotion.isAssignable(VexType.VectorType, VexType.IntType))
        assertTrue(VexTypePromotion.isAssignable(VexType.Vector2Type, VexType.FloatType))
        assertTrue(VexTypePromotion.isAssignable(VexType.Vector4Type, VexType.FloatType))

        // Vector to Scalar is not allowed
        assertFalse(VexTypePromotion.isAssignable(VexType.FloatType, VexType.VectorType))
        assertFalse(VexTypePromotion.isAssignable(VexType.IntType, VexType.VectorType))

        // Vector to different Vector is not allowed
        assertFalse(VexTypePromotion.isAssignable(VexType.VectorType, VexType.Vector2Type))
        assertFalse(VexTypePromotion.isAssignable(VexType.Vector2Type, VexType.Vector4Type))

        // Scalar to Matrix is allowed
        assertTrue(VexTypePromotion.isAssignable(VexType.MatrixType, VexType.FloatType))
        assertTrue(VexTypePromotion.isAssignable(VexType.Matrix3Type, VexType.FloatType))

        // Matrix to Scalar is not allowed
        assertFalse(VexTypePromotion.isAssignable(VexType.FloatType, VexType.MatrixType))

        // Matrix to different Matrix is not allowed
        assertFalse(VexTypePromotion.isAssignable(VexType.MatrixType, VexType.Matrix3Type))

        // Matrix and Vector incompatibility
        assertFalse(VexTypePromotion.isAssignable(VexType.MatrixType, VexType.VectorType))
        assertFalse(VexTypePromotion.isAssignable(VexType.VectorType, VexType.MatrixType))
    }

    @Test
    fun testPromoteAdditive() {
        // String concatenation
        assertEquals(
            VexType.StringType,
            VexTypePromotion.promote(
                VexType.StringType,
                VexType.StringType,
                VexTypePromotion.OperatorKind.ADDITIVE,
            ),
        )
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.StringType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.ADDITIVE,
            ),
        )
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.FloatType,
                VexType.StringType,
                VexTypePromotion.OperatorKind.ADDITIVE,
            ),
        )

        // Numeric addition
        assertEquals(
            VexType.IntType,
            VexTypePromotion.promote(
                VexType.IntType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.ADDITIVE,
            ),
        )
        assertEquals(
            VexType.FloatType,
            VexTypePromotion.promote(
                VexType.IntType,
                VexType.FloatType,
                VexTypePromotion.OperatorKind.ADDITIVE,
            ),
        )
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.VectorType,
                VexType.FloatType,
                VexTypePromotion.OperatorKind.ADDITIVE,
            ),
        )
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.VectorType,
                VexType.VectorType,
                VexTypePromotion.OperatorKind.ADDITIVE,
            ),
        )
    }

    @Test
    fun testPromoteSubtractive() {
        // Subtraction with strings is not allowed
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.StringType,
                VexType.StringType,
                VexTypePromotion.OperatorKind.SUBTRACTIVE,
            ),
        )
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.StringType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.SUBTRACTIVE,
            ),
        )

        // Numeric subtraction
        assertEquals(
            VexType.IntType,
            VexTypePromotion.promote(
                VexType.IntType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.SUBTRACTIVE,
            ),
        )
        assertEquals(
            VexType.FloatType,
            VexTypePromotion.promote(
                VexType.FloatType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.SUBTRACTIVE,
            ),
        )
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.VectorType,
                VexType.FloatType,
                VexTypePromotion.OperatorKind.SUBTRACTIVE,
            ),
        )
    }

    @Test
    fun testPromoteMultiplicative() {
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.StringType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )

        // Matrix * Matrix
        assertEquals(
            VexType.MatrixType,
            VexTypePromotion.promote(
                VexType.MatrixType,
                VexType.MatrixType,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )
        assertEquals(
            VexType.Matrix3Type,
            VexTypePromotion.promote(
                VexType.Matrix3Type,
                VexType.Matrix3Type,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )

        // Vector * Matrix -> Vector
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.VectorType,
                VexType.MatrixType,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.MatrixType,
                VexType.VectorType,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.VectorType,
                VexType.Matrix3Type,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )

        // Vector * Scalar -> Vector
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.VectorType,
                VexType.FloatType,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )
        assertEquals(
            VexType.VectorType,
            VexTypePromotion.promote(
                VexType.FloatType,
                VexType.VectorType,
                VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            ),
        )
    }

    @Test
    fun testPromoteBitwiseAndShift() {
        // Bitwise and shift are only valid between integers
        assertEquals(
            VexType.IntType,
            VexTypePromotion.promote(
                VexType.IntType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.BITWISE,
            ),
        )
        assertEquals(
            VexType.IntType,
            VexTypePromotion.promote(
                VexType.IntType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.SHIFT,
            ),
        )

        // Float or other types are invalid
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.FloatType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.BITWISE,
            ),
        )
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.IntType,
                VexType.FloatType,
                VexTypePromotion.OperatorKind.SHIFT,
            ),
        )
        assertEquals(
            VexType.UnknownType,
            VexTypePromotion.promote(
                VexType.StringType,
                VexType.IntType,
                VexTypePromotion.OperatorKind.BITWISE,
            ),
        )
    }

    @Test
    fun testGetOperatorKind() {
        assertEquals(
            VexTypePromotion.OperatorKind.ADDITIVE,
            VexTypePromotion.getOperatorKind(VexTypes.PLUS),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.ADDITIVE,
            VexTypePromotion.getOperatorKind(VexTypes.PLUSEQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.SUBTRACTIVE,
            VexTypePromotion.getOperatorKind(VexTypes.MINUS),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.SUBTRACTIVE,
            VexTypePromotion.getOperatorKind(VexTypes.MINUSEQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            VexTypePromotion.getOperatorKind(VexTypes.MUL),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            VexTypePromotion.getOperatorKind(VexTypes.MULEQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            VexTypePromotion.getOperatorKind(VexTypes.DIV),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            VexTypePromotion.getOperatorKind(VexTypes.DIVEQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            VexTypePromotion.getOperatorKind(VexTypes.MOD),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.MULTIPLICATIVE,
            VexTypePromotion.getOperatorKind(VexTypes.MODEQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.BITWISE,
            VexTypePromotion.getOperatorKind(VexTypes.BITAND),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.BITWISE,
            VexTypePromotion.getOperatorKind(VexTypes.ANDEQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.BITWISE,
            VexTypePromotion.getOperatorKind(VexTypes.BITOR),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.BITWISE,
            VexTypePromotion.getOperatorKind(VexTypes.OREQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.BITWISE,
            VexTypePromotion.getOperatorKind(VexTypes.BITXOR),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.BITWISE,
            VexTypePromotion.getOperatorKind(VexTypes.XOREQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.SHIFT,
            VexTypePromotion.getOperatorKind(VexTypes.LSHIFT),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.SHIFT,
            VexTypePromotion.getOperatorKind(VexTypes.LSHIFTEQ),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.SHIFT,
            VexTypePromotion.getOperatorKind(VexTypes.RSHIFT),
        )
        assertEquals(
            VexTypePromotion.OperatorKind.SHIFT,
            VexTypePromotion.getOperatorKind(VexTypes.RSHIFTEQ),
        )
        assertNull(VexTypePromotion.getOperatorKind(VexTypes.EQUALS))
        assertNull(VexTypePromotion.getOperatorKind(VexTypes.COMMA))
    }
}
