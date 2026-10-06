package com.chiiraac.migasto.ui

import com.chiiraac.migasto.domain.Money
import com.chiiraac.migasto.ui.components.niceCeiling
import com.chiiraac.migasto.ui.movement.sanitizeAmount
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiHelpersTest {
    @Test
    fun `amount field keeps digits and separators without reinterpreting them`() {
        assertEquals("12,505", sanitizeAmount("12,505"))
        assertEquals("1.500", sanitizeAmount("1.500"))
        assertEquals("45", sanitizeAmount("4a5€"))
        assertEquals(14, sanitizeAmount("1".repeat(30)).length)
    }

    @Test
    fun `thousands separators typed in the field reach the parser intact`() {
        // Antes "1.500" se guardaba como 1,50 €.
        val es = Locale.forLanguageTag("es-ES")
        assertEquals(150_000L, Money.parseToCents(sanitizeAmount("1.500"), es))
        assertEquals(123_456L, Money.parseToCents(sanitizeAmount("1.234,56"), es))
        assertEquals(1_250L, Money.parseToCents(sanitizeAmount("12,5"), es))
        assertNull(Money.parseToCents(sanitizeAmount("12,505"), es))
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
