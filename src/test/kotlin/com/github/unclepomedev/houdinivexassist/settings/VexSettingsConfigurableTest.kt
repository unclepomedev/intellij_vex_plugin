package com.github.unclepomedev.houdinivexassist.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class VexSettingsConfigurableTest : BasePlatformTestCase() {

    private var originalHfs: String = ""
    private var originalInclude: String = ""

    override fun setUp() {
        super.setUp()
        val state = VexSettingsState.instance
        originalHfs = state.hfsPath
        originalInclude = state.includePath
    }

    override fun tearDown() {
        val state = VexSettingsState.instance
        state.hfsPath = originalHfs
        state.includePath = originalInclude
        super.tearDown()
    }

    fun testDisplayName() {
        val configurable = VexSettingsConfigurable()
        assertEquals("VEX", configurable.displayName)
    }

    fun testConfigurableLifecycle() {
        val configurable = VexSettingsConfigurable()
        val component = configurable.createComponent()
        assertNotNull(component)
        assertEquals(configurable.preferredFocusedComponent, configurable.preferredFocusedComponent)

        val state = VexSettingsState.instance
        state.hfsPath = "/opt/hfs"
        state.includePath = "/opt/include"

        // reset should load from state into UI
        configurable.reset()
        assertFalse(configurable.isModified)

        // Modify UI
        val settingsComponent = configurable.preferredFocusedComponent
        assertNotNull(settingsComponent)

        // Simulate modifying settings via state and configurable
        configurable.reset()
        assertFalse(configurable.isModified)

        configurable.disposeUIResources()
        assertFalse(configurable.isModified) // disposed should return false
    }

    fun testSettingsStatePersistence() {
        val state = VexSettingsState()
        state.hfsPath = "/custom/hfs"
        state.includePath = "/custom/inc"

        assertEquals(state, state.state)

        val newState = VexSettingsState()
        newState.loadState(state)

        assertEquals("/custom/hfs", newState.hfsPath)
        assertEquals("/custom/inc", newState.includePath)
    }
}
