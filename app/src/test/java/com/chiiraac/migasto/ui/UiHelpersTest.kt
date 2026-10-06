package com.chiiraac.migasto.ui

import com.chiiraac.migasto.ui.components.niceCeiling
import com.chiiraac.migasto.ui.movement.sanitizeAmount
import org.junit.Assert.assertEquals
import org.junit.Test

class UiHelpersTest {
    @Test
    fun `amount field keeps one separator and two decimals`() {
        assertEquals("12,50", sanitizeAmount("12,505"))
        assertEquals("12.53", sanitizeAmount("12.5.3"))
        assertEquals("1234567", sanitizeAmount("123456789"))
        assertEquals("45", sanitizeAmount("4a5€"))
    }

    @Test
    fun `chart scale rounds up to nice numbers`() {
        assertEquals(100L, niceCeiling(0))
        assertEquals(1_000L, niceCeiling(1_000))
        assertEquals(2_000L, niceCeiling(1_178))
        assertEquals(250_000L, niceCeiling(245_000))
        assertEquals(500_000L, niceCeiling(301_000))
        assertEquals(1_000_000L, niceCeiling(650_000))
    }
}
