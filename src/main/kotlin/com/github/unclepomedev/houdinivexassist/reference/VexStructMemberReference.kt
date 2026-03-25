package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexElementFactory
import com.github.unclepomedev.houdinivexassist.psi.VexMemberExpr
import com.github.unclepomedev.houdinivexassist.psi.VexScopeAnalyzer
import com.github.unclepomedev.houdinivexassist.types.VexType
import com.github.unclepomedev.houdinivexassist.types.VexTypeInference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VexStructMemberReference(
    element: VexMemberExpr,
    textRange: TextRange
) : PsiReferenceBase<VexMemberExpr>(element, textRange) {

    override fun resolve(): PsiElement? {
        val memberName = element.identifier?.text ?: return null
        val baseExpr = element.expr
        val baseType = VexTypeInference.inferType(baseExpr)

        if (baseType is VexType.StructType) {
            val structName = baseType.name
            val structDefs = VexScopeAnalyzer.getVisibleStructs(element)
            val targetStruct = structDefs.find { it.identifier?.text == structName } ?: return null

            for (member in targetStruct.structMemberList) {
                val matchedDecl = member.declarationItemList.find { it.identifier.text == memberName }
                if (matchedDecl != null) {
                    return matchedDecl
                }
            }
        }
        return null
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val id = element.identifier ?: return element
        val newId = VexElementFactory.createIdentifier(element.project, newElementName)
        id.replace(newId)
        return element
    }
}
