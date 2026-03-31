package com.github.unclepomedev.houdinivexassist.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "com.github.unclepomedev.houdinivexassist.settings.VexSettingsState",
    storages = [Storage("VexPluginSettings.xml")]
)
class VexSettingsState : PersistentStateComponent<VexSettingsState> {
    var includePath: String = ""
    var hfsPath: String = ""

    override fun getState(): VexSettingsState = this

    override fun loadState(state: VexSettingsState) {
        this.includePath = state.includePath
        this.hfsPath = state.hfsPath
    }

    companion object {
        val instance: VexSettingsState
            get() = ApplicationManager.getApplication().getService(VexSettingsState::class.java)
    }
}
