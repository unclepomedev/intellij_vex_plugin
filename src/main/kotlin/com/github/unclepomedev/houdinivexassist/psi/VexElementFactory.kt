package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

object VexElementFactory {
    fun createIdentifier(project: Project, name: String): PsiElement {
        val file = createVexFile(project, "int $name;")
        val declarationItem = PsiTreeUtil.findChildOfType(file, VexDeclarationItem::class.java)
        return declarationItem!!.identifier
    }

    private fun createVexFile(project: Project, text: String): VexFile {
        return PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.vfl", VexLanguage.INSTANCE, text) as VexFile
    }
}
