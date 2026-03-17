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

    private fun extractFromDeclarationItem(item: VexDeclarationItem): VexType {
        val typeString = resolveDeclarationBaseTypeString(item) ?: return VexType.UnknownType
        val baseType = VexType.fromString(typeString)

        return wrapInArrayIfNeeded(baseType, item)
    }

    private fun extractFromParameterDef(paramDef: VexParameterDef): VexType {
        val typeString = paramDef.firstChild?.text ?: return VexType.UnknownType
        val baseType = VexType.fromString(typeString)

        return wrapInArrayIfNeeded(baseType, paramDef)
    }

    private fun extractFromFunctionDef(funcDef: VexFunctionDef): VexType {
        var child = funcDef.firstChild
        while (child != null) {
            if (isReturnTypeIdentifier(child)) {
                return VexType.fromString(child.text)
            }
            child = child.nextSibling
        }

        return VexType.UnknownType
    }

    private fun resolveDeclarationBaseTypeString(item: VexDeclarationItem): String? {
        return when (val parent = item.parent) {
            is VexDeclarationStatement -> parent.firstChild?.text
            is VexStructMember -> parent.firstChild?.text
            else -> null
        }
    }

    private fun isReturnTypeIdentifier(element: PsiElement): Boolean {
        val elementType = element.node.elementType
        return element !is PsiWhiteSpace &&
                elementType != VexTypes.EXPORT &&
                elementType != VexTypes.FUNCTION
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
