package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

private fun PsiElement.getLastIdentifier(): PsiElement? {
    val identifiers = this.node.getChildren(TokenSet.create(VexTypes.IDENTIFIER))
    return identifiers.lastOrNull()?.psi
}

val VexDeclarationItem.identifier: PsiElement
    get() = this.getLastIdentifier()!!

val VexFunctionDef.identifier: PsiElement
    get() = this.getLastIdentifier()!!

val VexParameterDef.identifier: PsiElement
    get() = this.getLastIdentifier()!!

val VexParameterSig.identifier: PsiElement?
    get() = this.getLastIdentifier()

val VexStructDef.identifier: PsiElement?
    get() = this.getLastIdentifier()

val VexStructMember.identifier: PsiElement?
    get() = this.getLastIdentifier()