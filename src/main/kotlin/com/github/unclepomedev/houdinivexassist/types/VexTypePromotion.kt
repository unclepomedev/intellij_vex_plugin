package com.github.unclepomedev.houdinivexassist.types

object VexTypePromotion {

    enum class OperatorKind { ADDITIVE, MULTIPLICATIVE, BITWISE, SHIFT }

    fun promote(t1: VexType, t2: VexType, operatorKind: OperatorKind): VexType {
        if (t1 == VexType.UnknownType) return t2
        if (t2 == VexType.UnknownType) return t1

        return when (operatorKind) {
            OperatorKind.ADDITIVE -> promoteAdditive(t1, t2)
            OperatorKind.MULTIPLICATIVE -> promoteMultiplicative(t1, t2)
            OperatorKind.BITWISE, OperatorKind.SHIFT -> promoteBitwiseOrShift(t1, t2)
        }
    }

    private fun promoteAdditive(t1: VexType, t2: VexType): VexType {
        if (t1 == VexType.StringType || t2 == VexType.StringType) {
            return VexType.StringType
        }

        return promoteNumeric(t1, t2)
    }

    private fun promoteMultiplicative(t1: VexType, t2: VexType): VexType {
        if (t1 == VexType.StringType || t2 == VexType.StringType) {
            return VexType.UnknownType
        }

        if (t1 == VexType.MatrixType && t2 == VexType.MatrixType) return VexType.MatrixType
        if (t1 == VexType.Matrix3Type && t2 == VexType.Matrix3Type) return VexType.Matrix3Type

        val types = setOf(t1, t2)
        if (types.contains(VexType.VectorType) && types.contains(VexType.MatrixType)) return VexType.VectorType
        if (types.contains(VexType.VectorType) && types.contains(VexType.Matrix3Type)) return VexType.VectorType

        return promoteNumeric(t1, t2)
    }

    private fun promoteBitwiseOrShift(t1: VexType, t2: VexType): VexType {
        if (t1 == VexType.IntType && t2 == VexType.IntType) {
            return VexType.IntType
        }
        return VexType.UnknownType
    }

    private fun promoteNumeric(t1: VexType, t2: VexType): VexType {
        if (t1 == t2) return t1

        val types = setOf(t1, t2)

        if (types.contains(VexType.IntType) && types.contains(VexType.FloatType)) {
            return VexType.FloatType
        }

        if (isVector(t1) || isVector(t2)) {
            val vectorType = types.firstOrNull { isVector(it) } ?: return VexType.UnknownType
            val otherType = types.first { it != vectorType }
            return if (isScalar(otherType)) vectorType else VexType.UnknownType
        }

        if (isMatrix(t1) || isMatrix(t2)) {
            val matrixType = types.firstOrNull { isMatrix(it) } ?: return VexType.UnknownType
            val otherType = types.first { it != matrixType }
            return if (isScalar(otherType)) matrixType else VexType.UnknownType
        }

        return VexType.UnknownType
    }

    private fun isVector(type: VexType): Boolean {
        return type == VexType.Vector2Type || type == VexType.VectorType || type == VexType.Vector4Type
    }

    private fun isMatrix(type: VexType): Boolean {
        return type == VexType.Matrix3Type || type == VexType.MatrixType
    }

    private fun isScalar(type: VexType): Boolean {
        return type == VexType.IntType || type == VexType.FloatType
    }
}
