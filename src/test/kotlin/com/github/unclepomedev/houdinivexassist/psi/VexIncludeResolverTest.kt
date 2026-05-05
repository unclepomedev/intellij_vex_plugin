package com.github.unclepomedev.houdinivexassist.psi

import org.junit.Assert.assertEquals
import org.junit.Test

class VexIncludeResolverTest {

    @Test
    fun testParseIncludePathsWithUnixSeparator() {
        val pathStr = "/usr/include:/opt/houdini/vex/include:&"
        val parsed = VexIncludeResolver.parseIncludePaths(pathStr, ":")
        assertEquals(listOf("/usr/include", "/opt/houdini/vex/include"), parsed)
    }

    @Test
    fun testParseIncludePathsWithWindowsSeparator() {
        val pathStr = "C:\\Program Files\\Side Effects Software\\Houdini\\vex\\include;D:\\my_vex_lib;&"
        // simulating the separator as Windows ;
        val parsed = VexIncludeResolver.parseIncludePaths(pathStr, ";")
        assertEquals(
            listOf("C:\\Program Files\\Side Effects Software\\Houdini\\vex\\include", "D:\\my_vex_lib"),
            parsed
        )
    }

    @Test
    fun testParseIncludePathsWithMixedSeparators() {
        // user setting ; but on a system where File.pathSeparator is :
        val pathStr = "/xxx/include;/yyy/include;&"
        val parsed = VexIncludeResolver.parseIncludePaths(pathStr, ":")
        assertEquals(listOf("/xxx/include", "/yyy/include"), parsed)
    }

    @Test
    fun testParseIncludePathsWithUrlScheme() {
        val pathStr = "temp:///xxx/include;file:///yyy/include;&"
        val parsed = VexIncludeResolver.parseIncludePaths(pathStr, ":")
        assertEquals(listOf("temp:///xxx/include", "file:///yyy/include"), parsed)
    }

    @Test
    fun testParseIncludePathsWindowsPathOnUnixSeparator() {
        // user setting Windows paths with ; but on a system where File.pathSeparator is :
        val pathStr = "C:\\path1;D:\\path2;&"
        val parsed = VexIncludeResolver.parseIncludePaths(pathStr, ":")
        assertEquals(listOf("C:\\path1", "D:\\path2"), parsed)
    }
}
