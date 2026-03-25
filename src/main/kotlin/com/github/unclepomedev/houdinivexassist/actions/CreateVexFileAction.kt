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
        private const val ACTION_TITLE = "VEX File"
        private const val ACTION_DESCRIPTION = "Creates a new VEX file"
        private const val DIALOG_TITLE = "New VEX File"
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(DIALOG_TITLE)
            .addKind("VEX Library file (.vfl)", VexIcons.FILE, "VexFile.vfl")
            .addKind("VEX Snippet (.vex)", VexIcons.FILE, "VexFile.vex")
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "Create VEX File: $newName"
    }
}
