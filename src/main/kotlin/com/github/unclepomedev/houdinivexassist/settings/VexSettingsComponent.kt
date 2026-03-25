package com.github.unclepomedev.houdinivexassist.settings

import com.github.unclepomedev.houdinivexassist.MyBundle
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class VexSettingsComponent {
    val panel: JPanel
    val includePathTextField = JBTextField()

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel(MyBundle.message("vex.settings.include.path.label")),
                includePathTextField,
                1,
                false
            )
            .addComponent(JBLabel(MyBundle.message("vex.settings.include.path.description")))
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}
