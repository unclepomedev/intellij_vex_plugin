package com.github.unclepomedev.houdinivexassist.lexer

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.psi.tree.IElementType

object VexLexerMacroHandler {

    private val includeRegex = Regex("^#[ \\t]*include([ \\t]+.*|)$")
    private val defineRegex = Regex("^#[ \\t]*define[ \\t]+[a-zA-Z_]\\w*.*$", RegexOption.DOT_MATCHES_ALL)
    private val ifdefRegex = Regex("^#[ \\t]*ifdef[ \\t]+[a-zA-Z_]\\w*.*$")
    private val ifndefRegex = Regex("^#[ \\t]*ifndef[ \\t]+[a-zA-Z_]\\w*.*$")
    private val undefRegex = Regex("^#[ \\t]*undef[ \\t]+[a-zA-Z_]\\w*.*$")
    private val ifRegex = Regex("^#[ \\t]*if([ \\t]+.*|[ \\t]*)$")
    private val elifRegex = Regex("^#[ \\t]*elif([ \\t]+.*|[ \\t]*)$")
    private val elseRegex = Regex("^#[ \\t]*else[ \\t]*(//.*|/\\*.*)?$")
    private val endifRegex = Regex("^#[ \\t]*endif[ \\t]*(//.*|/\\*.*)?$")

    enum class MacroKind {
        INCLUDE, DEFINE, IFDEF, IFNDEF, UNDEF, IF, ELIF, ELSE, ENDIF
    }

    data class MacroMatch(
        val kind: MacroKind,
        val type: IElementType,
        val keywordLength: Int,
        val nextState: Int
    )

    fun handleMacro(text: String): MacroMatch? {
        if (includeRegex.matches(text)) {
            val idx = text.indexOf("include")
            return MacroMatch(MacroKind.INCLUDE, VexTypes.INCLUDE_KW, idx + 7, VexLexer.IN_INCLUDE)
        }
        if (defineRegex.matches(text)) {
            val idx = text.indexOf("define")
            return MacroMatch(MacroKind.DEFINE, VexTypes.DEFINE_KW, idx + 6, VexLexer.IN_DEFINE)
        }
        if (ifdefRegex.matches(text)) {
            val idx = text.indexOf("ifdef")
            return MacroMatch(MacroKind.IFDEF, VexTypes.PP_IFDEF_KW, idx + 5, VexLexer.IN_PP_IDENTIFIER)
        }
        if (ifndefRegex.matches(text)) {
            val idx = text.indexOf("ifndef")
            return MacroMatch(MacroKind.IFNDEF, VexTypes.PP_IFNDEF_KW, idx + 6, VexLexer.IN_PP_IDENTIFIER)
        }
        if (undefRegex.matches(text)) {
            val idx = text.indexOf("undef")
            return MacroMatch(MacroKind.UNDEF, VexTypes.PP_UNDEF_KW, idx + 5, VexLexer.IN_PP_IDENTIFIER)
        }
        if (ifRegex.matches(text)) {
            val idx = text.indexOf("if")
            return MacroMatch(MacroKind.IF, VexTypes.PP_IF_KW, idx + 2, VexLexer.IN_DEFINE_BODY)
        }
        if (elifRegex.matches(text)) {
            val idx = text.indexOf("elif")
            return MacroMatch(MacroKind.ELIF, VexTypes.PP_ELIF_KW, idx + 4, VexLexer.IN_DEFINE_BODY)
        }
        if (elseRegex.matches(text)) {
            return MacroMatch(MacroKind.ELSE, VexTypes.PP_ELSE_KW, text.length, VexLexer.YYINITIAL)
        }
        if (endifRegex.matches(text)) {
            return MacroMatch(MacroKind.ENDIF, VexTypes.PP_ENDIF_KW, text.length, VexLexer.YYINITIAL)
        }

        return null
    }
}