package com.github.unclepomedev.houdinivexassist

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testDummy() {
        assertEquals("dummy", "dummy")
    }
}
