package com.chiiraac.migasto.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `parses amounts typed by the user`() {
        assertEquals(1200L, Money.parseToCents("12"))
        assertEquals(1250L, Money.parseToCents("12,5"))
        assertEquals(1250L, Money.parseToCents("12.50"))
        assertEquals(1205L, Money.parseToCents("12,05"))
        assertEquals(50L, Money.parseToCents(",5"))
        assertEquals(1200L, Money.parseToCents("12,"))
        assertEquals(123456L, Money.parseToCents("1.234,56"))
        assertEquals(123456L, Money.parseToCents("1,234.56"))
        assertEquals(123400L, Money.parseToCents("1.234"))
        assertEquals(999L, Money.parseToCents(" 9,99 € "))
    }

    @Test
    fun `rejects invalid amounts`() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("-5"))
        assertNull(Money.parseToCents("1,2345"))
        assertNull(Money.parseToCents("99999999999"))
        assertNull(Money.parseToCents(","))
    }

    @Test
    fun `formats euros with sign`() {
        val es = Locale.forLanguageTag("es-ES")
        val plain = Money.format(117800, es)
        assertTrue(plain, plain.contains("178,00") && plain.contains("€"))
        assertTrue(Money.format(117800, es, signed = true).startsWith("+"))
        assertTrue(Money.format(-4500, es).startsWith("-"))
        assertTrue(Money.format(-4500, es).contains("45,00"))
        assertEquals(Money.format(0, es), Money.format(0, es, signed = true))
    }

    @Test
    fun `input text round trips`() {
        val es = Locale.forLanguageTag("es-ES")
        assertEquals("12,50", Money.toInput(1250, es))
        assertEquals("12", Money.toInput(1200, es))
        assertEquals("0,05", Money.toInput(5, es))
        assertEquals(1250L, Money.parseToCents(Money.toInput(1250, es)))
    }
}
