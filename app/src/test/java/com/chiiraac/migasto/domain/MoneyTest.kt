package com.chiiraac.migasto.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    private val es = Locale.forLanguageTag("es-ES")

    @Test
    fun `parses amounts typed by the user`() {
        assertEquals(1200L, Money.parseToCents("12", es))
        assertEquals(1250L, Money.parseToCents("12,5", es))
        assertEquals(1250L, Money.parseToCents("12.50", es))
        assertEquals(1205L, Money.parseToCents("12,05", es))
        assertEquals(50L, Money.parseToCents(",5", es))
        assertEquals(1200L, Money.parseToCents("12,", es))
        assertEquals(123456L, Money.parseToCents("1.234,56", es))
        assertEquals(123456L, Money.parseToCents("1,234.56", es))
        assertEquals(123400L, Money.parseToCents("1.234", es))
        assertEquals(999L, Money.parseToCents(" 9,99 € ", es))
    }

    @Test
    fun `three digits after the separator depend on the language`() {
        assertEquals(150_000L, Money.parseToCents("1.500", es))
        assertNull(Money.parseToCents("12,505", es))
        assertEquals(150_000L, Money.parseToCents("1,500", Locale.US))
        assertNull(Money.parseToCents("1.500", Locale.US))
    }

    @Test
    fun `rejects invalid amounts`() {
        assertNull(Money.parseToCents("", es))
        assertNull(Money.parseToCents("abc", es))
        assertNull(Money.parseToCents("-5", es))
        assertNull(Money.parseToCents("1,2345", es))
        assertNull(Money.parseToCents("99999999999", es))
        assertNull(Money.parseToCents(",", es))
    }

    @Test
    fun `formats euros with sign`() {
        val plain = Money.format(117800, es)
        assertTrue(plain, plain.contains("178,00") && plain.contains("€"))
        assertTrue(Money.format(117800, es, signed = true).startsWith("+"))
        assertTrue(Money.format(-4500, es).startsWith("-"))
        assertTrue(Money.format(-4500, es).contains("45,00"))
        assertEquals(Money.format(0, es), Money.format(0, es, signed = true))
    }

    @Test
    fun `compact chart labels keep cents and round half up`() {
        assertEquals("0 €", Money.formatCompact(0, es))
        assertEquals("0,5 €", Money.formatCompact(50, es))
        assertEquals("1,25 €", Money.formatCompact(125, es))
        assertEquals("12,5 €", Money.formatCompact(1250, es))
        assertEquals("25 €", Money.formatCompact(2500, es))
        assertEquals("2 k€", Money.formatCompact(200_000, es))
    }

    @Test
    fun `input text only uses comma or dot as decimal separator`() {
        val arabic = Locale.forLanguageTag("ar-EG")
        val input = Money.toInput(1250, arabic)
        assertEquals(1250L, Money.parseToCents(input, arabic))
    }

    @Test
    fun `input text round trips`() {
        assertEquals("12,50", Money.toInput(1250, es))
        assertEquals("12", Money.toInput(1200, es))
        assertEquals("0,05", Money.toInput(5, es))
        assertEquals(1250L, Money.parseToCents(Money.toInput(1250, es), es))
    }
}
