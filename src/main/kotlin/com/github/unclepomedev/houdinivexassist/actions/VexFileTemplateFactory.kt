package com.github.unclepomedev.houdinivexassist.actions

import com.github.unclepomedev.houdinivexassist.icons.VexIcons
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class VexFileTemplateFactory : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("VEX", VexIcons.FILE)
        group.addTemplate(FileTemplateDescriptor("VexFile.vex", VexIcons.FILE))
        return group
    }
}
