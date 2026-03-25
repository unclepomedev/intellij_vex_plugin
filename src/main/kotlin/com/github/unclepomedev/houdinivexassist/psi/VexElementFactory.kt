package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

object VexElementFactory {
    fun createIdentifier(project: Project, name: String): PsiElement {
        val file = createVexFile(project, "int $name;")
        val declarationItem = PsiTreeUtil.findChildOfType(file, VexDeclarationItem::class.java)
            ?: throw IncorrectOperationException("Invalid identifier: $name")
        val identifier = declarationItem.identifier
        if (identifier.text != name) {
            throw IncorrectOperationException("Invalid identifier: $name")
        }
        return identifier
    }

    fun createIncludeDirective(project: Project, text: String): VexIncludeDirective {
        val file = createVexFile(project, "#include $text")
        return PsiTreeUtil.findChildOfType(file, VexIncludeDirective::class.java)
            ?: throw IncorrectOperationException("Invalid include string: $text")
    }

    private fun createVexFile(project: Project, text: String): VexFile {
        return PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.vfl", VexLanguage.INSTANCE, text) as VexFile
    }
}
