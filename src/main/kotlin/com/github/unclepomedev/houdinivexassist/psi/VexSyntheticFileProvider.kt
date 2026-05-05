package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

object VexSyntheticFileProvider {
    private val LOG = Logger.getInstance(VexSyntheticFileProvider::class.java)

    /**
     * Parses the given PsiFile (non-VEX such as .h) as a VexFile.
     * Returns null if the file cannot be parsed as VEX. Results are cached.
     */
    fun getAsVexFile(file: PsiFile): VexFile? {
        if (file is VexFile) return file

        return CachedValuesManager.getCachedValue(file) {
            val project = file.project
            val parsed = try {
                PsiFileFactory.getInstance(project)
                    .createFileFromText(file.name, VexLanguage.INSTANCE, file.text) as? VexFile
            } catch (e: Exception) {
                LOG.warn("Failed to parse ${file.name} as VexFile", e)
                null
            }

            val originalUrl = file.originalFile.virtualFile?.url
            if (parsed != null && originalUrl != null) {
                parsed.putUserData(VexFile.ORIGINAL_FILE_PATH_KEY, originalUrl)
            }

            CachedValueProvider.Result.create(parsed, file)
        }
    }
}
