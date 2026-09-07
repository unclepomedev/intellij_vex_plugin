package com.github.unclepomedev.houdinivexassist.editor

import com.github.unclepomedev.houdinivexassist.psi.VexTypes
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class VexBraceMatcherTest : BasePlatformTestCase() {

    fun testPairs() {
        val matcher = VexBraceMatcher()
        val pairs = matcher.pairs
        assertEquals(3, pairs.size)

        val bracePair = pairs.find { it.leftBraceType == VexTypes.LBRACE }
        assertNotNull(bracePair)
        assertEquals(VexTypes.RBRACE, bracePair!!.rightBraceType)
        assertTrue(bracePair.isStructural)

        val parenPair = pairs.find { it.leftBraceType == VexTypes.LPAREN }
        assertNotNull(parenPair)
        assertEquals(VexTypes.RPAREN, parenPair!!.rightBraceType)
        assertFalse(parenPair.isStructural)

        val bracketPair = pairs.find { it.leftBraceType == VexTypes.LBRACK }
        assertNotNull(bracketPair)
        assertEquals(VexTypes.RBRACK, bracketPair!!.rightBraceType)
        assertFalse(bracketPair.isStructural)
    }

    fun testIsPairedBracesAllowedBeforeType() {
        val matcher = VexBraceMatcher()
        assertTrue(matcher.isPairedBracesAllowedBeforeType(VexTypes.LBRACE, null))
        assertTrue(matcher.isPairedBracesAllowedBeforeType(VexTypes.LBRACE, VexTypes.IDENTIFIER))
        assertTrue(matcher.isPairedBracesAllowedBeforeType(VexTypes.LPAREN, VexTypes.SEMICOLON))
    }

    fun testGetCodeConstructStart() {
        val matcher = VexBraceMatcher()
        assertEquals(42, matcher.getCodeConstructStart(null, 42))
        assertEquals(0, matcher.getCodeConstructStart(null, 0))
    }
}
