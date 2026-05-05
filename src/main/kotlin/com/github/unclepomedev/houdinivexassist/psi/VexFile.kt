package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile

class VexFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, VexLanguage.INSTANCE) {
    override fun getFileType(): FileType = VexFileType
    override fun toString(): String = "VEX File"

    companion object {
        val ORIGINAL_FILE_PATH_KEY = Key.create<String>("VEX_ORIGINAL_FILE_PATH")

        fun getFileKey(file: PsiFile): String {
            return file.getUserData(ORIGINAL_FILE_PATH_KEY)
                ?: file.originalFile.virtualFile?.path
                ?: file.name
        }
    }
}
