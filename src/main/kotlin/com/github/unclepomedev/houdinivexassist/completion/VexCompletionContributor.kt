package com.github.unclepomedev.houdinivexassist.completion

import com.github.unclepomedev.houdinivexassist.lang.VexLanguage
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.github.unclepomedev.houdinivexassist.services.VexApiProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class VexCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(VexTypes.IDENTIFIER).withLanguage(VexLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.originalFile.project
                    val vexApiProvider = project.getService(VexApiProvider::class.java) ?: return
                    val functions = vexApiProvider.functions
                    for (func in functions) {
                        val lookupElement = LookupElementBuilder.create(func.name)
                            .withTypeText(func.returnType)
                            .withTailText("(${func.args.joinToString(", ")})", true)
                            .withIcon(AllIcons.Nodes.Function)

                        result.addElement(lookupElement)
                    }
                }
            }
        )
    }
}
