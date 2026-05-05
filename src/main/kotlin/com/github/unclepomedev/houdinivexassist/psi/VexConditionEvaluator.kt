package com.github.unclepomedev.houdinivexassist.psi

object VexConditionEvaluator {
    // \b ensures "defined" is not part of a longer identifier (e.g. "definedX").
    private val definedRegex = Regex("""\bdefined\b\s*\(?\s*([a-zA-Z_]\w*)\s*\)?""")
    private val notDefinedRegex = Regex("""!\s*\bdefined\b\s*\(?\s*([a-zA-Z_]\w*)\s*\)?""")

    fun evaluate(conditionText: String?, definedMacros: Set<String>): Boolean {
        val text = conditionText?.trim()
        if (text.isNullOrEmpty()) return true

        // Numeric literal: treat 0 as false, any other integer (incl. 00, +0 sign-prefixed nonzero, -1) as truth value.
        // Using toIntOrNull avoids partial-match pitfalls like "0" vs "00" and handles signed forms uniformly.
        text.toIntOrNull()?.let { return it != 0 }

        notDefinedRegex.matchEntire(text)?.let { match ->
            val (macroName) = match.destructured
            return !definedMacros.contains(macroName)
        }

        definedRegex.matchEntire(text)?.let { match ->
            val (macroName) = match.destructured
            return definedMacros.contains(macroName)
        }

        // Default to true for unsupported complex expressions to avoid aggressive hiding
        return true
    }
}
