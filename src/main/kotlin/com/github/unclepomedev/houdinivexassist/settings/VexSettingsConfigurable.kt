package com.github.unclepomedev.houdinivexassist.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class VexSettingsConfigurable : Configurable {
    private var mySettingsComponent: VexSettingsComponent? = null

    override fun getDisplayName(): String = "VEX"

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.includePathTextField
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = VexSettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings = VexSettingsState.instance
        return mySettingsComponent?.includePathTextField?.text != settings.includePath
    }

    override fun apply() {
        val settings = VexSettingsState.instance
        settings.includePath = mySettingsComponent?.includePathTextField?.text ?: ""
    }

    override fun reset() {
        val settings = VexSettingsState.instance
        mySettingsComponent?.includePathTextField?.text = settings.includePath
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
