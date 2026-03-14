package com.github.unclepomedev.houdinivexassist.lang

import com.github.unclepomedev.houdinivexassist.lexer.VexLexer
import com.intellij.lexer.FlexAdapter

class VexLexerAdapter : FlexAdapter(VexLexer(null))
