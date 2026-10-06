package com.chiiraac.migasto.domain

import com.chiiraac.migasto.TestData
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
    private val day = LocalDate.of(2026, 10, 1)

    @Test
    fun `balance separates bank and cash and applies transfers`() {
        val movements = listOf(
            TestData.movement("Sueldo", 100_000, MovementType.INCOME, "salary", day),
            TestData.movement("Compra", 2_500, MovementType.EXPENSE, "groceries", day),
            TestData.movement("Luz", 5_000, MovementType.BILL, "electricity", day),
            TestData.movement("Cajero", 10_000, MovementType.TRANSFER, "transfer", day, PaymentMethod.BANK),
            TestData.movement("Pan", 300, MovementType.EXPENSE, "groceries", day, PaymentMethod.CASH),
        )
        val balance = Stats.balance(movements)
        assertEquals(100_000L - 2_500 - 5_000 - 10_000, balance.bankCents)
        assertEquals(10_000L - 300, balance.cashCents)
        assertEquals(100_000L - 2_500 - 5_000 - 300, balance.totalCents)
    }

    @Test
    fun `totals count bills as expenses and ignore transfers`() {
        val movements = listOf(
            TestData.movement("Sueldo", 100_000, MovementType.INCOME, "salary", day),
            TestData.movement("Luz", 5_000, MovementType.BILL, "electricity", day),
            TestData.movement("Ocio", 2_000, MovementType.EXPENSE, "leisure", day),
            TestData.movement("Cajero", 10_000, MovementType.TRANSFER, "transfer", day),
        )
        val totals = Stats.totals(movements)
        assertEquals(100_000L, totals.incomeCents)
        assertEquals(7_000L, totals.expenseCents)
        assertEquals(93_000L, totals.netCents)
    }

    @Test
    fun `daily series covers every day of the month`() {
        val series = Stats.dailySeries(TestData.movements, YearMonth.of(2026, 10))
        assertEquals(31, series.size)
        val first = series.first()
        assertEquals(1, first.date.dayOfMonth)
        assertEquals(145_000L, first.totals.incomeCents)
        assertTrue(first.totals.expenseCents > 0)
        assertEquals(0L, series[10].totals.incomeCents + series[10].totals.expenseCents)
    }

    @Test
    fun `category breakdown is sorted and shares add up to one`() {
        val categories = Stats.byCategory(Stats.inMonth(TestData.movements, YearMonth.of(2026, 10)))
        assertEquals("rent", categories.first().categoryId)
        assertEquals(1f, categories.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
        assertTrue(categories.zipWithNext().all { (a, b) -> a.cents >= b.cents })
    }

    @Test
    fun `last months series ends at the given month`() {
        val series = Stats.lastMonthsSeries(TestData.movements, YearMonth.of(2026, 10), 12)
        assertEquals(12, series.size)
        assertEquals(YearMonth.of(2026, 10), series.last().month)
        assertEquals(YearMonth.of(2025, 11), series.first().month)
    }

    @Test
    fun `activity by day flags income and expenses`() {
        val activity = Stats.activityByDay(TestData.movements, YearMonth.of(2026, 10))
        assertTrue(activity.getValue(2).hasIncome)
        assertFalse(activity.getValue(2).hasExpense)
        assertTrue(activity.getValue(1).hasExpense)
        assertTrue(activity.getValue(3).hasTransfer)
        assertFalse(activity.containsKey(6))
    }

    @Test
    fun `members totals use resolved names`() {
        val members = Stats.byMember(TestData.movements) { it.createdByName.uppercase() }
        assertEquals(setOf("EMIL", "LAURA"), members.map { it.name }.toSet())
    }
}
