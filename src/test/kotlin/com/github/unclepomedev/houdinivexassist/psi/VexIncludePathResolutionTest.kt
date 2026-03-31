package com.github.unclepomedev.houdinivexassist.psi

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.settings.VexSettingsState
import java.nio.file.Files

class VexIncludePathResolutionTest : VexTestBase() {

    private lateinit var savedHfsPath: String
    private lateinit var savedIncludePath: String

    override fun setUp() {
        super.setUp()
        val settings = VexSettingsState.instance
        savedHfsPath = settings.hfsPath
        savedIncludePath = settings.includePath
    }

    override fun tearDown() {
        try {
            val settings = VexSettingsState.instance
            settings.hfsPath = savedHfsPath
            settings.includePath = savedIncludePath
        } finally {
            super.tearDown()
        }
    }

    fun testStandardPathReplacement() {
        val tmpDir = Files.createTempDirectory("hfs_test")
        try {
            val settings = VexSettingsState.instance
            settings.hfsPath = tmpDir.toString()

            val expected = "${tmpDir}/houdini/vex/include"
            val result = VexScopeAnalyzer.parseIncludePaths("&;/other/path", ";")
            assertEquals(listOf(expected, "/other/path"), result)
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    fun testMacOSFrameworkPathReplacement() {
        val tmpDir = Files.createTempDirectory("hfs_test")
        try {
            val frameworkDir =
                tmpDir.resolve("Frameworks/Houdini.framework/Versions/Current/Resources/houdini/vex/include")
            Files.createDirectories(frameworkDir)

            val settings = VexSettingsState.instance
            settings.hfsPath = tmpDir.toString()

            val expected = frameworkDir.toString()
            val result = VexScopeAnalyzer.parseIncludePaths("&", ";")
            assertEquals(listOf(expected), result)
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    fun testEmptyHfsPath() {
        val settings = VexSettingsState.instance
        settings.hfsPath = ""

        val result = VexScopeAnalyzer.parseIncludePaths("&;/some/path", ";")
        assertEquals(listOf("/some/path"), result)
    }
}
