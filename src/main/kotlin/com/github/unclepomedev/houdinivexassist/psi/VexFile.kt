package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class VexFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, VexLanguage.INSTANCE) {
    override fun getFileType(): FileType = VexFileType
    override fun toString(): String = "VEX File"
}
