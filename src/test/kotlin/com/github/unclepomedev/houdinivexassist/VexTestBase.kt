package com.github.unclepomedev.houdinivexassist

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class VexTestBase : BasePlatformTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    override fun setUp() {
        super.setUp()
    }
}
