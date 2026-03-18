package com.github.unclepomedev.houdinivexassist.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateVexFileAction : CreateFileFromTemplateAction(
    "Vex File",
    "Creates a new Vex file",
    null // TODO icon
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Vex File")
            // TODO icon
            .addKind("VexFile", null, "VexFile.vex")
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "Create Vex File: $newName"
    }
}
