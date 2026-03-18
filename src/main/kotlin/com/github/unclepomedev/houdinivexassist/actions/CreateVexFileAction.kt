package com.github.unclepomedev.houdinivexassist.actions

import com.github.unclepomedev.houdinivexassist.icons.VexIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateVexFileAction : CreateFileFromTemplateAction(
    ACTION_TITLE,
    ACTION_DESCRIPTION,
    VexIcons.FILE
) {
    companion object {
        private const val ACTION_TITLE = "Vex File"
        private const val ACTION_DESCRIPTION = "Creates a new Vex file"
        private const val DIALOG_TITLE = "New Vex File"
        private const val KIND_TITLE = "Vex file"
        private const val TEMPLATE_NAME = "VexFile.vex"
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(DIALOG_TITLE)
            .addKind(KIND_TITLE, VexIcons.FILE, TEMPLATE_NAME)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "Create Vex File: $newName"
    }
}
