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

    private fun extractFromDeclarationItem(item: VexDeclarationItem): VexType {
        val typeString = resolveDeclarationBaseTypeString(item) ?: return VexType.UnknownType
        val baseType = VexType.fromString(typeString)

        return wrapInArrayIfNeeded(baseType, item)
    }

    private fun extractFromParameterDef(paramDef: VexParameterDef): VexType {
        val typeString = paramDef.typeRef.text ?: return VexType.UnknownType
        val baseType = VexType.fromString(typeString)

        return wrapInArrayIfNeeded(baseType, paramDef)
    }

    private fun extractFromFunctionDef(funcDef: VexFunctionDef): VexType {
        val typeString = funcDef.typeRef.text ?: return VexType.UnknownType
        return VexType.fromString(typeString)
    }

    private fun resolveDeclarationBaseTypeString(item: VexDeclarationItem): String? {
        return when (val parent = item.parent) {
            is VexDeclarationStatement -> parent.typeRef.text ?: return null
            is VexStructMember -> parent.typeRef.text ?: return null
            else -> null
        }
    }

    private fun wrapInArrayIfNeeded(baseType: VexType, element: PsiElement): VexType {
        return if (hasArrayBrackets(element)) {
            VexType.ArrayType(baseType)
        } else {
            baseType
        }
    }

    private fun hasArrayBrackets(element: PsiElement): Boolean {
        val hasLeftBracket = element.node.findChildByType(VexTypes.LBRACK) != null
        val hasRightBracket = element.node.findChildByType(VexTypes.RBRACK) != null
        return hasLeftBracket && hasRightBracket
    }
}
