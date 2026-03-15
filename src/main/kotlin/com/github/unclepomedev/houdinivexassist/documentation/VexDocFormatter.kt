package com.github.unclepomedev.houdinivexassist.documentation

import com.intellij.lang.documentation.DocumentationMarkup

/**
 * Formatter that converts Houdini's custom help text into IntelliJ standard HTML documentation.
 */
object VexDocFormatter {

    fun format(name: String, rawText: String): String {
        val usages = mutableListOf<String>()

        val bodyHtml = rawText
            .escapeHtml()
            .removeMetadata()
            .extractUsagesTo(usages)
            .applyTextDecorations()
            .formatCodeBlocks()
            .cleanupWhitespace()

        return buildMarkup(name, usages, bodyHtml)
    }

    private fun String.escapeHtml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun String.removeMetadata(): String = this
        .replace(Regex("(?m)^=\\s*.*\\s*=$"), "")
        .replace(Regex("(?m)^#.*$"), "")

    private fun String.extractUsagesTo(usages: MutableList<String>): String {
        return this.replace(Regex("(?m)^\\s*:usage:\\s*`([^`]+)`")) {
            usages.add(it.groupValues[1].trim())
            ""
        }
    }

    private fun String.applyTextDecorations(): String = this
        .replace(Regex("\"\"\"([\\s\\S]*?)\"\"\"")) { "<i>${it.groupValues[1].trim()}</i>" }
        .replace(Regex("\\[Vex:([^]]+)]")) { "<code>${it.groupValues[1]}</code>" }
        .replace(Regex("(?m)^\\s*@([a-zA-Z]+)")) { "<br><b>${it.groupValues[1].uppercase()}</b><hr>" }
        .replace(Regex("(?m)^\\s*:box:(.*)")) { "<br><b>${it.groupValues[1].trim()}</b>" }

    private fun String.formatCodeBlocks(): String {
        val parts = this.split(Regex("\\{\\{\\{\\s*#!vex"))
        val builder = StringBuilder()

        for ((index, part) in parts.withIndex()) {
            if (index == 0) {
                builder.append(part.cleanOutsideText())
            } else {
                val splitByEnd = part.split(Regex("}}}"))
                if (splitByEnd.size >= 2) {
                    val codeContent = splitByEnd[0].trimIndent().trim()
                    builder.append("<pre><code>$codeContent</code></pre>")
                    builder.append(splitByEnd.drop(1).joinToString("}}}").cleanOutsideText())
                } else {
                    builder.append(part.cleanOutsideText())
                }
            }
        }
        return builder.toString()
    }

    private fun String.cleanOutsideText(): String = this
        .lines().joinToString("<br>") { it.trim() }

    private fun String.cleanupWhitespace(): String = this
        .replace(Regex("(<br>\\s*){3,}"), "<br><br>")
        .replace(Regex("<hr>\\s*(<br>\\s*)+"), "<hr>")
        .replace(Regex("(<br>\\s*)+<pre>"), "<pre>")
        .replace(Regex("^\\s*(<br>\\s*)+"), "")

    private fun buildMarkup(name: String, usages: List<String>, bodyHtml: String): String {
        val sb = StringBuilder()

        sb.append(DocumentationMarkup.DEFINITION_START)
        if (usages.isNotEmpty()) {
            sb.append(usages.joinToString("<br>"))
        } else {
            sb.append("<b>$name</b>")
        }
        sb.append(DocumentationMarkup.DEFINITION_END)

        sb.append(DocumentationMarkup.CONTENT_START)
        sb.append(bodyHtml.trim())
        sb.append(DocumentationMarkup.CONTENT_END)

        return sb.toString()
    }
}
