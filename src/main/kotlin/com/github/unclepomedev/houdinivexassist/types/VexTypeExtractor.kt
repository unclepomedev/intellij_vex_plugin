package com.github.unclepomedev.houdinivexassist.types

import com.github.unclepomedev.houdinivexassist.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

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
        val typeString = when (val parent = item.parent) {
            is VexDeclarationStatement -> parent.firstChild?.text
            is VexStructMember -> parent.firstChild?.text
            // VexForInit is private in BNF, but it falls back to VexDeclarationStatement or VexExprStatement, so don't need to handle it separately.
            else -> null
        } ?: return VexType.UnknownType

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
        return VexType.fromString(typeString)
    }

    /**
     * Extracts the return type from a function definition (e.g., float myFunc()).
     */
    private fun extractFromFunctionDef(funcDef: VexFunctionDef): VexType {
        var child = funcDef.firstChild
        while (child != null) {
            val elementType = child.node.elementType
            if (child !is PsiWhiteSpace &&
                elementType != VexTypes.EXPORT &&
                elementType != VexTypes.FUNCTION
            ) {
                return VexType.fromString(child.text)
            }
            child = child.nextSibling
        }

        return VexType.UnknownType
    }
}
