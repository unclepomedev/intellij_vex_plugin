package com.github.unclepomedev.houdinivexassist.psi.impl

import com.github.unclepomedev.houdinivexassist.psi.VexIncludeDirective
import com.github.unclepomedev.houdinivexassist.reference.VexIncludeReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference

abstract class VexIncludeDirectiveMixin(node: ASTNode) : ASTWrapperPsiElement(node), VexIncludeDirective {
    override fun getReference(): PsiReference? {
        val stringNode = string ?: unclosedString ?: sysString ?: unclosedSysString ?: return null
        return VexIncludeReference(this, stringNode.textRangeInParent)
    }
}
