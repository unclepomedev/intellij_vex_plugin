package com.github.unclepomedev.houdinivexassist.types

object VexTypePromotion {

    fun promote(t1: VexType, t2: VexType): VexType {
        if (t1 == t2) return t1
        if (t1 == VexType.UnknownType) return t2
        if (t2 == VexType.UnknownType) return t1

        val types = setOf(t1, t2)

        if (types.contains(VexType.IntType) && types.contains(VexType.FloatType)) {
            return VexType.FloatType
        }

        if (types.contains(VexType.StringType)) {
            return VexType.StringType
        }

        if (isVector(t1) || isVector(t2)) {
            return promoteVectorAndScalar(types)
        }

        if (isMatrix(t1) || isMatrix(t2)) {
            return promoteMatrixAndScalar(types)
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

    private fun promoteVectorAndScalar(types: Set<VexType>): VexType {
        val vectorType = types.firstOrNull { isVector(it) } ?: return VexType.UnknownType
        val otherType = types.first { it != vectorType }

        return if (isScalar(otherType)) vectorType else VexType.UnknownType
    }

    private fun promoteMatrixAndScalar(types: Set<VexType>): VexType {
        val matrixType = types.firstOrNull { isMatrix(it) } ?: return VexType.UnknownType
        val otherType = types.first { it != matrixType }

        return if (isScalar(otherType)) matrixType else VexType.UnknownType
    }
}
