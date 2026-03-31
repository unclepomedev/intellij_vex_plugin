package com.github.unclepomedev.houdinivexassist.settings

import com.github.unclepomedev.houdinivexassist.MyBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.util.ResourceBundle
import javax.swing.JPanel

class VexSettingsComponent {
    val panel: JPanel
    val hfsPathField = TextFieldWithBrowseButton()
    val includePathTextField = JBTextField()

    companion object {
        private val bundle = ResourceBundle.getBundle("messages.MyBundle")
        fun getRawMessage(key: String): String = bundle.getString(key)
    }

    init {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = MyBundle.message("vex.settings.hfs.path.dialog.title")
            description = getRawMessage("vex.settings.hfs.path.description")
        }
        hfsPathField.addBrowseFolderListener(TextBrowseFolderListener(descriptor))

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel(MyBundle.message("vex.settings.hfs.path.label")),
                hfsPathField,
                1,
                false
            )
            .addComponent(JBLabel(getRawMessage("vex.settings.hfs.path.description")))
            .addSeparator()
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
