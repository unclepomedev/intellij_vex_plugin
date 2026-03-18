package com.github.unclepomedev.houdinivexassist.actions

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.intellij.ide.fileTemplates.FileTemplateManager

class VexFileTemplateTest : VexTestBase() {

    fun testVexFileTemplateExists() {
        val templateManager = FileTemplateManager.getInstance(project)
        val template = templateManager.getInternalTemplate("VexFile.vex")
        assertNotNull("Vex File template should be registered", template)
    }
}
