package com.github.unclepomedev.houdinivexassist.psi

object VexConditionEvaluator {

    private val definedRegex = Regex("""defined\s*\(?\s*([a-zA-Z_]\w*)\s*\)?""")
    private val notDefinedRegex = Regex("""!\s*defined\s*\(?\s*([a-zA-Z_]\w*)\s*\)?""")

    fun evaluate(conditionText: String?, definedMacros: Set<String>): Boolean {
        val text = conditionText?.trim()
        if (text.isNullOrEmpty()) return true

        if (text == "0") return false
        if (text == "1") return true

        notDefinedRegex.find(text)?.let { return !definedMacros.contains(it.groupValues[1]) }
        definedRegex.find(text)?.let { return definedMacros.contains(it.groupValues[1]) }

        // Default to true for unsupported complex expressions to avoid aggressive hiding
        return true
    }
}
