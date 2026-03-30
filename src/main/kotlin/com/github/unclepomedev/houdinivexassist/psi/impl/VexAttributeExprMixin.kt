package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexAttributeExpr
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.intellij.lang.ASTNode

abstract class VexAttributeExprMixin(node: ASTNode) : VexExprImpl(node), VexAttributeExpr {

    /**
     * Infers [VexType] from the explicit cast prefix of this attribute expression.
     *
     * Examples:
     * - `f@Cd`   -> FloatType
     * - `v[]@P`  -> ArrayType(VectorType)
     * - `@ptnum` -> null (no prefix)
     */
    fun inferCastPrefixType(): VexType? {
        val text = text ?: return null
        val atIndex = text.indexOf('@')
        if (atIndex <= 0) return null

        val prefix = text.substring(0, atIndex)
        val isArray = prefix.endsWith("[]")
        val typeChar = prefix[0]

        val baseType = when (typeChar) {
            'f' -> VexType.FloatType
            'i' -> VexType.IntType
            'v' -> VexType.VectorType
            'u' -> VexType.Vector2Type
            'p' -> VexType.Vector4Type
            's' -> VexType.StringType
            'm', '3' -> VexType.Matrix3Type
            '4' -> VexType.MatrixType
            'd' -> VexType.DictType
            else -> VexType.UnknownType
        }

        return if (isArray) VexType.ArrayType(baseType) else baseType
    }
}
