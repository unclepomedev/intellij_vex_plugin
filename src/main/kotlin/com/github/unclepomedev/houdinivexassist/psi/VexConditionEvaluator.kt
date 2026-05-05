package com.github.unclepomedev.houdinivexassist.psi

object VexConditionEvaluator {
    private val DEFINED_REGEX = Regex("""^defined\s*\(?\s*([a-zA-Z_]\w*)\s*\)?$""")
    private val NOT_DEFINED_REGEX = Regex("""^!\s*defined\s*\(?\s*([a-zA-Z_]\w*)\s*\)?$""")

    fun evaluate(conditionText: String?, directive: VexPreprocessorDirective, isPreprocessorContext: Boolean = false): Boolean {
        val text = conditionText?.trim()
        if (text.isNullOrEmpty()) return true

        text.toIntOrNull()?.let { return it != 0 }

        DEFINED_REGEX.matchEntire(text)?.let { match ->
            val name = match.groupValues[1]
            return if (isPreprocessorContext) {
                VexMacroResolver.resolveMacroForPreprocessor(directive, name) != null
            } else {
                VexMacroResolver.resolveMacro(directive, name) != null
            }
        }

        NOT_DEFINED_REGEX.matchEntire(text)?.let { match ->
            val name = match.groupValues[1]
            val defined = if (isPreprocessorContext) {
                VexMacroResolver.resolveMacroForPreprocessor(directive, name) != null
            } else {
                VexMacroResolver.resolveMacro(directive, name) != null
            }
            return !defined
        }

        return true
    }
}
