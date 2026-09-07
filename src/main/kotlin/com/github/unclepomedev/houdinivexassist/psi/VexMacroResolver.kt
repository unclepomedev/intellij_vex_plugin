package com.github.unclepomedev.houdinivexassist.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object VexMacroResolver {
    val ORIGINAL_FILE_PATH_KEY = Key.create<String>("VEX_ORIGINAL_FILE_PATH")
    private val resolvingFiles = ThreadLocal.withInitial { mutableSetOf<String>() }

    fun resolveMacro(context: PsiElement, name: String): PsiElement? {
        val file = context.containingFile ?: return null
        return resolveInFile(file, file, name, context.textOffset)
    }

    private fun resolveInFile(
        file: PsiFile,
        sourceFile: PsiFile,
        name: String,
        maxOffsetExclusive: Int,
    ): VexMacroDef? {
        val key = getFileKey(file, sourceFile)
        val visited = resolvingFiles.get()
        if (!visited.add(key)) return null

        try {
            return collectPrecedingDirectives(file, maxOffsetExclusive)
                .mapNotNull { directive -> resolveDirective(directive, sourceFile, name) }
                .lastOrNull()
        } finally {
            visited.remove(key)
        }
    }

    private fun getFileKey(file: PsiFile, sourceFile: PsiFile): String =
        file.getUserData(ORIGINAL_FILE_PATH_KEY)
            ?: sourceFile.originalFile.virtualFile?.path
            ?: sourceFile.name

    private fun collectPrecedingDirectives(
        file: PsiFile,
        maxOffsetExclusive: Int,
    ): List<PsiElement> {
        val macroDefs = PsiTreeUtil.findChildrenOfType(file, VexMacroDef::class.java)
        val includeDirectives =
            PsiTreeUtil.findChildrenOfType(file, VexIncludeDirective::class.java)
        return (macroDefs + includeDirectives)
            .filter { it.textOffset < maxOffsetExclusive }
            .sortedBy { it.textOffset }
    }

    private fun resolveDirective(
        directive: PsiElement,
        sourceFile: PsiFile,
        name: String,
    ): VexMacroDef? =
        when (directive) {
            is VexMacroDef -> directive.takeIf { it.identifier?.text == name }
            is VexIncludeDirective -> resolveIncludeDirective(directive, sourceFile, name)
            else -> null
        }

    private fun resolveIncludeDirective(
        directive: VexIncludeDirective,
        sourceFile: PsiFile,
        name: String,
    ): VexMacroDef? {
        val includedPsi = VexScopeAnalyzer.resolveIncludeFile(directive, sourceFile) ?: return null
        val vexFile =
            (includedPsi as? VexFile) ?: VexScopeAnalyzer.getOrCreateSyntheticVexFile(includedPsi)
        return resolveInFile(vexFile, includedPsi, name, Int.MAX_VALUE)
    }
}
