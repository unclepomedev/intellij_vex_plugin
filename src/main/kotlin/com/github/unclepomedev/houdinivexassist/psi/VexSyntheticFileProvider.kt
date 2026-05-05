package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

object VexSyntheticFileProvider {

    /**
     * Parses and returns the specified PsiFile (non-VEX file such as .h) as a VexFile.
     * Results are cached and reused unless the original file is modified (to avoid heavy operations).
     */
    fun getAsVexFile(file: PsiFile): VexFile {
        if (file is VexFile) return file

        return CachedValuesManager.getCachedValue(file) {
            val project = file.project
            val parsed = PsiFileFactory.getInstance(project)
                .createFileFromText(file.name, VexLanguage.INSTANCE, file.text) as VexFile

            val originalUrl = file.originalFile.virtualFile?.url
            if (originalUrl != null) {
                parsed.putUserData(VexFile.ORIGINAL_FILE_PATH_KEY, originalUrl)
            }

            CachedValueProvider.Result.create(parsed, file)
        }
    }
}
