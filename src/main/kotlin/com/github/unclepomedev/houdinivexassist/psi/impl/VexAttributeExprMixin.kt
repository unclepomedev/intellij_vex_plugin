package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexAttributeExpr
import com.github.unclepomedev.houdinivexassist.services.VexBuiltinVariableProvider
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.intellij.lang.ASTNode
import com.intellij.openapi.components.service

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

    /**
     * Infers the full [VexType] for this attribute expression.
     *
     * 1. If an explicit cast prefix exists, use it.
     * 2. Otherwise, look up the base name in [VexBuiltinVariableProvider].
     * 3. If not found, return [VexType.UnknownType].
     */
    fun inferType(): VexType {
        inferCastPrefixType()?.let { return it }

        val text = text ?: return VexType.UnknownType
        val atIndex = text.indexOf('@')
        if (atIndex < 0) return VexType.UnknownType

        val baseName = text.substring(atIndex + 1)
        if (baseName.isEmpty()) return VexType.UnknownType

        val provider = project.service<VexBuiltinVariableProvider>()
        return provider.getBuiltinType(baseName) ?: VexType.UnknownType
    }
}
