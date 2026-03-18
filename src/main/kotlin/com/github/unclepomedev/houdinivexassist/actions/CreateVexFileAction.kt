package com.github.unclepomedev.houdinivexassist.actions

import com.github.unclepomedev.houdinivexassist.icons.VexIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class CreateVexFileAction : CreateFileFromTemplateAction(
    "Vex File",
    "Creates a new Vex file",
    VexIcons.FILE
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Vex File")
            .addKind("Vex file", VexIcons.FILE, "VexFile.vex")
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "Create Vex File: $newName"
    }
}
