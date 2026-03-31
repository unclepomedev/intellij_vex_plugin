package com.github.unclepomedev.houdinivexassist.settings

import com.github.unclepomedev.houdinivexassist.MyBundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class VexSettingsConfigurable : Configurable {
    private var mySettingsComponent: VexSettingsComponent? = null

    override fun getDisplayName(): String = MyBundle.message("vex.settings.display.name")

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.hfsPathField
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = VexSettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val component = mySettingsComponent ?: return false
        val state = VexSettingsState.instance
        return component.hfsPathField.text != state.hfsPath ||
                component.includePathTextField.text != state.includePath
    }

    override fun apply() {
        val component = mySettingsComponent ?: return
        val state = VexSettingsState.instance
        state.hfsPath = component.hfsPathField.text
        state.includePath = component.includePathTextField.text
    }

    override fun reset() {
        val component = mySettingsComponent ?: return
        val state = VexSettingsState.instance
        component.hfsPathField.text = state.hfsPath
        component.includePathTextField.text = state.includePath
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
