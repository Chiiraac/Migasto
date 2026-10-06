package com.chiiraac.migasto.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

object Dates {

    /** "02 oct" */
    fun shortDay(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("dd MMM", locale).format(date).replace(".", "")

    /** "2 oct 2026" */
    fun mediumDay(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(date).replace(".", "")

    /** "martes, 6 de octubre de 2026" según el idioma. */
    fun longDay(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL).withLocale(locale).format(date)
            .replaceFirstChar { it.titlecase(locale) }

    /** "Octubre 2026" */
    fun monthTitle(month: YearMonth, locale: Locale = Locale.getDefault()): String {
        val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .let { if (it.all { c -> c.isDigit() }) month.month.getDisplayName(TextStyle.FULL, locale) else it }
        return "${name.replaceFirstChar { it.titlecase(locale) }} ${month.year}"
    }

    /** "oct" */
    fun shortMonth(month: YearMonth, locale: Locale = Locale.getDefault()): String {
        val name = month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
            .let { if (it.all { c -> c.isDigit() }) month.month.getDisplayName(TextStyle.SHORT, locale) else it }
        return name.replace(".", "").replaceFirstChar { it.titlecase(locale) }
    }

    fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

    /** Días de la semana en orden, empezando por [first]. */
    fun weekDays(first: DayOfWeek): List<DayOfWeek> = (0L until 7L).map { first.plus(it) }

    /** Inicial del día: L M X J V S D en español. */
    fun weekDayInitial(day: DayOfWeek, locale: Locale = Locale.getDefault()): String =
        day.getDisplayName(TextStyle.NARROW_STANDALONE, locale)
            .let { if (it.length > 2 || it.all { c -> c.isDigit() }) day.getDisplayName(TextStyle.NARROW, locale) else it }
            .uppercase(locale)

    /**
     * Celdas del calendario de un mes: null para los huecos antes del día 1 y
     * después del último día (completando semanas enteras).
     */
    fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val cells = ArrayList<LocalDate?>(42)
        repeat(leading) { cells += null }
        for (day in 1..month.lengthOfMonth()) cells += month.atDay(day)
        while (cells.size % 7 != 0) cells += null
        return cells
    }
}
