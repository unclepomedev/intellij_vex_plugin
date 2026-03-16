package com.github.unclepomedev.houdinivexassist.types

/**
 * A Sealed Class representing all types in the VEX language.
 * It does not depend on the Syntax Tree (PSI) and has only the concept of pure types.
 */
sealed class VexType {
    abstract val displayName: String

    // embedded basic types
    object IntType : VexType() {
        override val displayName = "int"
    }

    object FloatType : VexType() {
        override val displayName = "float"
    }

    object Vector2Type : VexType() {
        override val displayName = "vector2"
    }

    object VectorType : VexType() {
        override val displayName = "vector"
    }

    object Vector4Type : VexType() {
        override val displayName = "vector4"
    }

    object Matrix3Type : VexType() {
        override val displayName = "matrix3"
    }

    object MatrixType : VexType() {
        override val displayName = "matrix"
    }

    object StringType : VexType() {
        override val displayName = "string"
    }

    object VoidType : VexType() {
        override val displayName = "void"
    }

    object BsdfType : VexType() {
        override val displayName = "bsdf"
    }

    object DictType : VexType() {
        override val displayName = "dict"
    }

    // recursively defined array type
    data class ArrayType(val elementType: VexType) : VexType() {
        override val displayName = "${elementType.displayName}[]"
    }

    // user-defined struct type
    data class StructType(val name: String) : VexType() {
        override val displayName = "struct $name"
    }

    // error fallback
    object UnknownType : VexType() {
        override val displayName = "<unknown>"
    }

    companion object {
        /**
         * Converts a string of a type obtained from a PSI (e.g., "int", "vector") into a VexType object.
         */
        fun fromString(typeName: String): VexType {
            return when (typeName) {
                "int" -> IntType
                "float" -> FloatType
                "vector2" -> Vector2Type
                "vector" -> VectorType
                "vector4" -> Vector4Type
                "matrix3" -> Matrix3Type
                "matrix" -> MatrixType
                "string" -> StringType
                "void" -> VoidType
                "bsdf" -> BsdfType
                "dict" -> DictType
                else -> UnknownType // TODO: struct, etc.
            }
        }
    }
}
