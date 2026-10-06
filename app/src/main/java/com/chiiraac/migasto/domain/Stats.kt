package com.chiiraac.migasto.domain

import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/** Saldo por cuenta. */
data class Balance(val bankCents: Long, val cashCents: Long) {
    val totalCents: Long get() = bankCents + cashCents
}

/** Ingresos y gastos (las facturas cuentan como gasto; los traspasos no cuentan). */
data class Totals(val incomeCents: Long, val expenseCents: Long) {
    val netCents: Long get() = incomeCents - expenseCents

    operator fun plus(other: Totals) = Totals(incomeCents + other.incomeCents, expenseCents + other.expenseCents)

    companion object {
        val ZERO = Totals(0, 0)
    }
}

data class DayTotals(val date: LocalDate, val totals: Totals)
data class MonthTotals(val month: YearMonth, val totals: Totals)
data class CategoryTotal(val categoryId: String, val cents: Long, val share: Float)
data class MemberTotal(val uid: String, val name: String, val totals: Totals)

/** Efecto de un movimiento sobre el saldo de cada cuenta. */
fun Movement.signedEffect(account: PaymentMethod): Long = when (type) {
    MovementType.INCOME -> if (method == account) amountCents else 0
    MovementType.EXPENSE, MovementType.BILL -> if (method == account) -amountCents else 0
    MovementType.TRANSFER -> when (account) {
        method -> -amountCents
        method.other -> amountCents
        else -> 0
    }
}

/** Importe con signo tal y como se muestra en la lista (+ ingreso, - gasto, 0 traspaso). */
val Movement.signedAmount: Long
    get() = when (type) {
        MovementType.INCOME -> amountCents
        MovementType.EXPENSE, MovementType.BILL -> -amountCents
        MovementType.TRANSFER -> 0
    }

object Stats {

    fun balance(movements: Iterable<Movement>): Balance {
        var bank = 0L
        var cash = 0L
        for (m in movements) {
            bank += m.signedEffect(PaymentMethod.BANK)
            cash += m.signedEffect(PaymentMethod.CASH)
        }
        return Balance(bank, cash)
    }

    fun totals(movements: Iterable<Movement>): Totals {
        var income = 0L
        var expense = 0L
        for (m in movements) {
            when {
                m.type == MovementType.INCOME -> income += m.amountCents
                m.type.isOutflow -> expense += m.amountCents
            }
        }
        return Totals(income, expense)
    }

    fun inMonth(movements: Iterable<Movement>, month: YearMonth): List<Movement> =
        movements.filter { YearMonth.from(it.date) == month }

    fun inYear(movements: Iterable<Movement>, year: Int): List<Movement> =
        movements.filter { it.date.year == year }

    fun onDay(movements: Iterable<Movement>, day: LocalDate): List<Movement> =
        movements.filter { it.date == day }

    /** Totales de cada día del mes (incluye días sin movimientos). */
    fun dailySeries(movements: Iterable<Movement>, month: YearMonth): List<DayTotals> {
        val byDay = inMonth(movements, month).groupBy { it.date.dayOfMonth }
        return (1..month.lengthOfMonth()).map { day ->
            DayTotals(month.atDay(day), totals(byDay[day].orEmpty()))
        }
    }

    /** Totales de cada mes del año (enero a diciembre). */
    fun monthlySeries(movements: Iterable<Movement>, year: Int): List<MonthTotals> {
        val byMonth = inYear(movements, year).groupBy { it.date.monthValue }
        return Month.entries.map { month ->
            MonthTotals(YearMonth.of(year, month), totals(byMonth[month.value].orEmpty()))
        }
    }

    /** Totales de los últimos [count] meses terminando en [endMonth]. */
    fun lastMonthsSeries(movements: Iterable<Movement>, endMonth: YearMonth, count: Int): List<MonthTotals> {
        val byMonth = movements.groupBy { YearMonth.from(it.date) }
        return (count - 1 downTo 0).map { offset ->
            val month = endMonth.minusMonths(offset.toLong())
            MonthTotals(month, totals(byMonth[month].orEmpty()))
        }
    }

    /** Gasto (o ingreso) por categoría, de mayor a menor, con su porcentaje. */
    fun byCategory(movements: Iterable<Movement>, income: Boolean = false): List<CategoryTotal> {
        val relevant = movements.filter { if (income) it.type == MovementType.INCOME else it.type.isOutflow }
        val total = relevant.sumOf { it.amountCents }
        if (total == 0L) return emptyList()
        return relevant.groupBy { it.categoryId }
            .map { (category, list) ->
                val cents = list.sumOf { it.amountCents }
                CategoryTotal(category, cents, cents.toFloat() / total)
            }
            .sortedByDescending { it.cents }
    }

    /** Ingresos y gastos registrados por cada miembro. */
    fun byMember(movements: Iterable<Movement>, nameOf: (Movement) -> String): List<MemberTotal> =
        movements.filter { it.type != MovementType.TRANSFER }
            .groupBy { it.createdById }
            .map { (uid, list) -> MemberTotal(uid, nameOf(list.first()), totals(list)) }
            .sortedByDescending { it.totals.expenseCents + it.totals.incomeCents }

    /** Días del mes con movimientos, indicando si hubo ingresos y/o gastos. */
    fun activityByDay(movements: Iterable<Movement>, month: YearMonth): Map<Int, DayActivity> =
        inMonth(movements, month).groupBy { it.date.dayOfMonth }.mapValues { (_, list) ->
            DayActivity(
                hasIncome = list.any { it.type == MovementType.INCOME },
                hasExpense = list.any { it.type.isOutflow },
                hasTransfer = list.any { it.type == MovementType.TRANSFER },
            )
        }
}

data class DayActivity(val hasIncome: Boolean, val hasExpense: Boolean, val hasTransfer: Boolean)
