package com.github.unclepomedev.houdinivexassist.editor

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler

class VexQuoteHandler : SimpleTokenSetQuoteHandler(
    VexTypes.STRING,
    VexTypes.UNCLOSED_STRING
)
