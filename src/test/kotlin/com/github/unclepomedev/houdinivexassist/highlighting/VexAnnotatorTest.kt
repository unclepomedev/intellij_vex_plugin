package com.github.unclepomedev.houdinivexassist.highlighting

import com.github.unclepomedev.houdinivexassist.VexTestBase
import com.github.unclepomedev.houdinivexassist.lang.VexFileType

class VexAnnotatorTest : VexTestBase() {

    fun testValidFunctionIsNotHighlightedAsError() {
        myFixture.configureByText(VexFileType, "float d = distance(p1, p2);")
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testUnknownFunctionIsHighlightedAsError() {
        myFixture.configureByText(
            VexFileType,
            "float d = <error descr=\"Unknown VEX function: 'hogehoge'\">hogehoge</error>(p1, p2);"
        )
        myFixture.checkHighlighting(false, false, false, true)
    }

    fun testLocalFunctionIsNotHighlightedAsError() {
        myFixture.configureByText(
            VexFileType,
            """
            void myLocalFunc() {
                // do something
            }
            
            float d = myLocalFunc();
            """.trimIndent()
        )

        myFixture.checkHighlighting(false, false, false, true)
    }
}
