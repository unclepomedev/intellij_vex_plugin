package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.psi.PsiElement

object VexTypeExtractor {

    /**
     * Extracts the VexType from PSI elements such as variable declarations, parameter definitions, and function definitions.
     * Returns [VexType.UnknownType] if extraction is not possible.
     */
    fun extractType(element: PsiElement): VexType {
        return when (element) {
            is VexDeclarationItem -> extractFromDeclarationItem(element)
            is VexParameterDef -> extractFromParameterDef(element)
            is VexFunctionDef -> extractFromFunctionDef(element)
            else -> VexType.UnknownType
        }
    }

    /**
     * Extracts the type from a variable declaration (e.g., int a = 1;).
     */
    private fun extractFromDeclarationItem(item: VexDeclarationItem): VexType {
        val statement = item.parent as? VexDeclarationStatement ?: return VexType.UnknownType

        val typeString = statement.firstChild?.text ?: return VexType.UnknownType
        val baseType = VexType.fromString(typeString)
        val isArray = item.node.findChildByType(VexTypes.LBRACK) != null &&
                item.node.findChildByType(VexTypes.RBRACK) != null

        return if (isArray) {
            VexType.ArrayType(baseType)
        } else {
            baseType
        }
    }

    /**
     * Extract the type from the parameter definition (e.g., vector pos).
     */
    private fun extractFromParameterDef(paramDef: VexParameterDef): VexType {
        val typeString = paramDef.firstChild?.text ?: return VexType.UnknownType
        val baseType = VexType.fromString(typeString)
        return baseType
    }

    /**
     * Extracts the return type from a function definition (e.g., float myFunc()).
     */
    private fun extractFromFunctionDef(funcDef: VexFunctionDef): VexType {
        val typeNode = funcDef.node.findChildByType(VexTypes.TYPE) ?: return VexType.UnknownType
        return VexType.fromString(typeNode.text)
    }
}
