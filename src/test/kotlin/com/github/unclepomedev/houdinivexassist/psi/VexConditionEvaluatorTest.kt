package com.github.unclepomedev.houdinivexassist.psi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexConditionEvaluatorTest {

    private val defined = setOf("FOO", "BAR")

    @Test
    fun `null or empty condition is active`() {
        assertTrue(VexConditionEvaluator.evaluate(null, defined))
        assertTrue(VexConditionEvaluator.evaluate("", defined))
        assertTrue(VexConditionEvaluator.evaluate("   ", defined))
    }

    @Test
    fun `numeric literals - zero is false, non-zero is true`() {
        assertEquals(false, VexConditionEvaluator.evaluate("0", defined))
        assertEquals(false, VexConditionEvaluator.evaluate("00", defined))
        assertEquals(false, VexConditionEvaluator.evaluate("+0", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("1", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("-1", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("42", defined))
    }

    @Test
    fun `defined operator matches both forms`() {
        assertEquals(true, VexConditionEvaluator.evaluate("defined(FOO)", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("defined FOO", defined))
        assertEquals(false, VexConditionEvaluator.evaluate("defined(MISSING)", defined))
    }

    @Test
    fun `not defined operator`() {
        assertEquals(false, VexConditionEvaluator.evaluate("!defined(FOO)", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("!defined(MISSING)", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("! defined MISSING", defined))
    }

    @Test
    fun `identifier starting with defined is not treated as the operator`() {
        // "definedX" is an identifier, not the operator. Falls back to true (unsupported).
        assertEquals(true, VexConditionEvaluator.evaluate("definedX", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("definedFOO(BAR)", defined))
    }

    @Test
    fun `complex unsupported expressions default to true`() {
        assertEquals(true, VexConditionEvaluator.evaluate("FOO && BAR", defined))
        assertEquals(true, VexConditionEvaluator.evaluate("defined(FOO) && defined(BAR)", defined))
    }
}
