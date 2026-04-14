package com.github.unclepomedev.houdinivexassist.editor

import com.github.unclepomedev.houdinivexassist.lang.VexFileType
import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class VexCommenterTest : BasePlatformTestCase() {

    fun testCommenterReturnsLineCommentPrefix() {
        val commenter = VexCommenter()
        assertEquals("//", commenter.lineCommentPrefix)
    }

    fun testCommenterReturnsBlockCommentPrefix() {
        val commenter = VexCommenter()
        assertEquals("/*", commenter.blockCommentPrefix)
    }

    fun testCommenterReturnsBlockCommentSuffix() {
        val commenter = VexCommenter()
        assertEquals("*/", commenter.blockCommentSuffix)
    }

    fun testLineCommentTokenType() {
        val commenter = VexCommenter()
        assertEquals(VexTypes.LINE_COMMENT, commenter.lineCommentTokenType)
    }

    fun testBlockCommentTokenType() {
        val commenter = VexCommenter()
        assertEquals(VexTypes.BLOCK_COMMENT, commenter.blockCommentTokenType)
    }

    fun testLineCommentTokenTypeIsDifferentFromBlockCommentTokenType() {
        val commenter = VexCommenter()
        assertNotSame(commenter.lineCommentTokenType, commenter.blockCommentTokenType)
    }

    fun testDocumentationCommentTokenTypeIsNull() {
        val commenter = VexCommenter()
        assertNull(commenter.documentationCommentTokenType)
    }

    fun testIsDocumentationCommentReturnsFalse() {
        val commenter = VexCommenter()
        val code = "// line comment"
        val file = myFixture.configureByText(VexFileType, code)
        val comment = file.firstChild
        if (comment is com.intellij.psi.PsiComment) {
            assertFalse(commenter.isDocumentationComment(comment))
        }
    }
}
