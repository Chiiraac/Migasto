package com.chiiraac.migasto.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatesTest {
    private val es = Locale.forLanguageTag("es-ES")

    @Test
    fun `october 2026 starts on thursday in a monday-first grid`() {
        val grid = Dates.monthGrid(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(35, grid.size)
        (0 until 3).forEach { assertNull(grid[it]) }
        assertEquals(LocalDate.of(2026, 10, 1), grid[3])
        assertEquals(LocalDate.of(2026, 10, 31), grid[33])
        assertNull(grid[34])
    }

    @Test
    fun `sunday-first grid shifts by one`() {
        val grid = Dates.monthGrid(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 10, 1), grid[4])
    }

    @Test
    fun `spanish labels`() {
        assertEquals("02 oct", Dates.shortDay(LocalDate.of(2026, 10, 2), es))
        assertEquals("Octubre 2026", Dates.monthTitle(YearMonth.of(2026, 10), es))
        assertEquals(DayOfWeek.MONDAY, Dates.firstDayOfWeek(es))
        assertEquals(
            listOf("L", "M", "X", "J", "V", "S", "D"),
            Dates.weekDays(DayOfWeek.MONDAY).map { Dates.weekDayInitial(it, es) },
        )
    }
}
