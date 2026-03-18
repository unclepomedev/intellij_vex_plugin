package com.github.unclepomedev.houdinivexassist.actions

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class VexFileTemplateFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("VEX", null)
        // TODO icon
        group.addTemplate(FileTemplateDescriptor("VexFile.vex", null))
        return group
    }
}
