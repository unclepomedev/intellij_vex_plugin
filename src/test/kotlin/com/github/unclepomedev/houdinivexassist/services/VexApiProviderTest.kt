package com.github.unclepomedev.houdinivexassist.services

import com.github.unclepomedev.houdinivexassist.VexTestBase

class VexApiProviderTest : VexTestBase() {

    fun testApiProviderLoadsFunctions() {
        val provider = project.getService(VexApiProvider::class.java)
        assertNotNull("VexApiProvider service should be registered", provider)
        val functions = provider.functions

        assertNotNull("Functions list should not be null", functions)
        assertTrue(
            "Functions list should not be empty. Make sure vex_api_dump.json is generated.",
            functions.isNotEmpty()
        )

        val distanceFunc = functions.find { it.name == "distance" }
        assertNotNull("Should contain 'distance' function", distanceFunc)

        assertTrue("distance function should have arguments", distanceFunc!!.args.isNotEmpty())
        assertTrue("distance function should have a return type", distanceFunc.returnType.isNotEmpty())
    }
}
