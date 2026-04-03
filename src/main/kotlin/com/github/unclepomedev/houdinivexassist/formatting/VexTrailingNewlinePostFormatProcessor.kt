package com.github.unclepomedev.houdinivexassist.formatting

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor

class VexTrailingNewlinePostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source.fileType != VexFileType) return rangeToReformat
        val document = source.viewProvider.document ?: return rangeToReformat
        val text = document.text
        if (text.isNotEmpty() && !text.endsWith("\n")) {
            document.insertString(document.textLength, "\n")
            PsiDocumentManager.getInstance(source.project).commitDocument(document)
            return TextRange(rangeToReformat.startOffset, rangeToReformat.endOffset + 1)
        }
        return rangeToReformat
    }
}
