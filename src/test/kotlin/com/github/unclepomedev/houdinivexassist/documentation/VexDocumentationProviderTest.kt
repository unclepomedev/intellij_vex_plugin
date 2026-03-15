package com.github.unclepomedev.houdinivexassist.documentation

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType

class VexDocumentationProviderTest : VexTestBase() {

    fun testDocumentationGeneration() {
        myFixture.configureByText(VexFileType, "dist<caret>ance(ptnum, pos);")

        val originalElement = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull("Original element should not be null", originalElement)

        val provider = VexDocumentationProvider()
        val doc = provider.generateDoc(originalElement!!, originalElement)

        assertNotNull("Documentation should not be null for 'distance'", doc)
        assertTrue("Documentation should contain the function name", doc!!.contains("distance"))

        // Negative path: Variable declaration
        myFixture.configureByText(VexFileType, "float dist<caret>ance = 0;")
        val varElement = myFixture.file.findElementAt(myFixture.caretOffset)
        val varDoc = provider.generateDoc(varElement!!, varElement)
        assertNull("Documentation should be null for a non-call identifier", varDoc)
    }
}
