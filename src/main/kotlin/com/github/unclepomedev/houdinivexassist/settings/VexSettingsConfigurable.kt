package com.github.unclepomedev.houdinivexassist.settings

import com.github.unclepomedev.houdinivexassist.MyBundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class VexSettingsConfigurable : Configurable {
    private var mySettingsComponent: VexSettingsComponent? = null

    override fun getDisplayName(): String = MyBundle.message("vex.settings.display.name")

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.includePathTextField
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = VexSettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val component = mySettingsComponent ?: return false
        return component.includePathTextField.text != VexSettingsState.instance.includePath
    }

    override fun apply() {
        val component = mySettingsComponent ?: return
        VexSettingsState.instance.includePath = component.includePathTextField.text
    }

    override fun reset() {
        val settings = VexSettingsState.instance
        mySettingsComponent?.includePathTextField?.text = settings.includePath
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
