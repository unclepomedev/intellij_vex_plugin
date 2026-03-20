package com.github.unclepomedev.houdinivexassist.editor

import com.github.unclepomedev.houdinivexassist.psi.VexCallExpr
import com.github.unclepomedev.houdinivexassist.psi.VexFunctionResolver
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class VexInlayHintsProvider : InlayHintsProvider<VexInlayHintsProvider.Settings> {

    data class Settings(var showParameterHints: Boolean = true)

    override val key: SettingsKey<Settings> = SettingsKey("vex.inlay.hints")
    override val name: String = "VEX Parameter Hints"
    override val previewText: String = "void myFunc(int a, float b) {}\nvoid main() {\n    myFunc(1, 2.0);\n}"

    override fun createSettings(): Settings = Settings()

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JPanel {
                val panel = JPanel()
                val checkbox = javax.swing.JCheckBox(mainCheckboxText, settings.showParameterHints)
                checkbox.addActionListener {
                    settings.showParameterHints = checkbox.isSelected
                    listener.settingsChanged()
                }
                panel.add(checkbox)
                return panel
            }
            override val mainCheckboxText: String = "Show parameter hints"
        }
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        if (!settings.showParameterHints) return null

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (element !is VexCallExpr) return true

                val argumentList = element.argumentList ?: return true
                val args = argumentList.exprList
                if (args.isEmpty()) return true

                val parameters = VexFunctionResolver.resolveParameterNames(element)

                for (i in args.indices) {
                    if (i < parameters.size) {
                        val paramName = parameters[i]
                        // Format the hint text
                        val presentation = factory.seq(
                            factory.smallText("$paramName:"),
                            factory.textSpacePlaceholder(1, true)
                        )
                        sink.addInlineElement(
                            args[i].textRange.startOffset,
                            true,
                            presentation,
                            false
                        )
                    }
                }

                return true
            }
        }
    }
}
