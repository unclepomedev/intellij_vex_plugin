package com.github.unclepomedev.houdinivexassist.reference

import com.github.unclepomedev.houdinivexassist.psi.VexElementFactory
import com.github.unclepomedev.houdinivexassist.psi.VexIncludeDirective
import com.github.unclepomedev.houdinivexassist.psi.VexScopeAnalyzer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class VexIncludeReference(
    element: VexIncludeDirective,
    textRange: TextRange
) : PsiReferenceBase<VexIncludeDirective>(element, textRange) {

    override fun resolve(): PsiElement? {
        return VexScopeAnalyzer.resolveIncludeFile(element)
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val stringNode = element.string ?: element.unclosedString ?: return element
        val oldText = stringNode.text

        val startQuote = when {
            oldText.startsWith("\"") -> "\""
            oldText.startsWith("'") -> "'"
            else -> "\""
        }
        val isClosed = oldText.length > 1 && oldText.endsWith(startQuote)

        val innerPath = oldText.removePrefix(startQuote).let { if (isClosed) it.removeSuffix(startQuote) else it }
        val splitIndex = maxOf(innerPath.lastIndexOf('/'), innerPath.lastIndexOf('\\'))
        val dirPath = if (splitIndex != -1) innerPath.substring(0, splitIndex + 1) else ""

        val newInclude = VexElementFactory.createIncludeDirective(
            element.project,
            buildString {
                append(startQuote)
                append(dirPath)
                append(newElementName)
                if (isClosed) append(startQuote)
            }
        )
        val newStringNode = newInclude.string ?: newInclude.unclosedString
        if (newStringNode != null) {
            stringNode.replace(newStringNode)
        }
        return element
    }
}
