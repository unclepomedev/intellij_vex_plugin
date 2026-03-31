package com.github.unclepomedev.houdinivexassist.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import java.awt.Container

class VexSettingsComponentTest : BasePlatformTestCase() {

    private fun findLabels(container: Container): List<JBLabel> {
        val result = mutableListOf<JBLabel>()
        for (component in container.components) {
            if (component is JBLabel) result.add(component)
            if (component is Container) result.addAll(findLabels(component))
        }
        return result
    }

    fun testHfsDescriptionContainsAmpersand() {
        val settingsComponent = VexSettingsComponent()
        val labels = findLabels(settingsComponent.panel)
        val descLabel = labels.first { it.text.contains("symbol") }
        assertTrue(
            "Label text should contain '&' character, but was: ${descLabel.text}",
            descLabel.text.contains("&")
        )
    }
}
