package com.github.unclepomedev.houdinivexassist.documentation

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class VexDocumentationProvider : AbstractDocumentationProvider() {

    private val docCache = ConcurrentHashMap<String, String>()

    override fun getCustomDocumentationElement(
        editor: Editor, file: PsiFile, contextElement: PsiElement?, targetOffset: Int
    ): PsiElement? {
        if (
            contextElement?.node?.elementType == VexTypes.IDENTIFIER &&
            contextElement.parent is VexCallExpr &&
            (contextElement.parent as VexCallExpr).identifier == contextElement
        ) {
            return contextElement
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val target = originalElement ?: element
        val name = extractName(target) ?: return null

        return docCache.computeIfAbsent(name) {
            val path = "vex_help/functions/$it.txt"
            val stream = javaClass.classLoader.getResourceAsStream(path) ?: return@computeIfAbsent ""

            val helpText = InputStreamReader(stream, StandardCharsets.UTF_8).use { reader -> reader.readText() }
            VexDocFormatter.format(it, helpText)
        }.takeIf { it.isNotEmpty() }
    }

    private fun extractName(element: PsiElement): String? {
        val parent = element.parent
        if (parent is VexCallExpr && parent.identifier == element) {
            return element.text
        }
        return null
    }
}
