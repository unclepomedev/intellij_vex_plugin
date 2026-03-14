package com.github.unclepomedev.houdinivexassist

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object VexFileType : LanguageFileType(VexLanguage.INSTANCE) {
    override fun getName(): String = "VEX File"
    override fun getDescription(): String = "Houdini VEX language file"
    override fun getDefaultExtension(): String = "vfl"
    override fun getIcon(): Icon? = null
}
