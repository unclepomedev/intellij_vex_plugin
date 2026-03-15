package com.github.unclepomedev.houdinivexassist.documentation

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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

        val cachedDoc = docCache[name]
        if (cachedDoc != null) {
            return cachedDoc
        }

        val path = "vex_help/functions/$name.txt"
        val helpText = javaClass.classLoader.getResourceAsStream(path)?.use { stream ->
            stream.bufferedReader(StandardCharsets.UTF_8).readText()
        } ?: return null

        val formattedDoc = VexDocFormatter.format(name, helpText)
        docCache[name] = formattedDoc
        return formattedDoc
    }

    private fun extractName(element: PsiElement): String? {
        val parent = element.parent
        if (parent is VexCallExpr && parent.identifier == element) {
            return element.text
        }
        return null
    }
}
