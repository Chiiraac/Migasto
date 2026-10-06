package com.chiiraac.migasto.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.domain.Dates
import com.chiiraac.migasto.domain.Money
import com.chiiraac.migasto.domain.Stats
import com.chiiraac.migasto.domain.Totals
import com.chiiraac.migasto.ui.components.BarGroup
import com.chiiraac.migasto.ui.components.Categories
import com.chiiraac.migasto.ui.components.DonutChart
import com.chiiraac.migasto.ui.components.DonutSlice
import com.chiiraac.migasto.ui.components.EmptyState
import com.chiiraac.migasto.ui.components.IncomeExpenseBarChart
import com.chiiraac.migasto.ui.components.MemberAvatar
import com.chiiraac.migasto.ui.components.PeriodSwitcher
import com.chiiraac.migasto.ui.components.SectionCard
import com.chiiraac.migasto.ui.components.ShareBar
import com.chiiraac.migasto.ui.components.icon
import com.chiiraac.migasto.ui.components.label
import com.chiiraac.migasto.ui.components.today
import com.chiiraac.migasto.ui.main.MainUiState
import com.chiiraac.migasto.ui.theme.AppTheme
import java.text.NumberFormat
import java.time.YearMonth

enum class StatsPeriod { MONTH, YEAR, ALL }

private data class ChartEntry(val bar: BarGroup, val detailTitle: String)

@Composable
fun StatsScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    initialPeriod: StatsPeriod = StatsPeriod.MONTH,
) {
    val todayDate = today()
    val currentMonth = YearMonth.from(todayDate)
    var period by rememberSaveable { mutableStateOf(initialPeriod) }
    var monthText by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    var year by rememberSaveable { mutableIntStateOf(todayDate.year) }
    val month = YearMonth.parse(monthText)
    var selectedBar by remember(period, monthText, year) { mutableStateOf<Int?>(null) }

    val all = state.movements
    val periodMovements = remember(all, period, month, year) {
        when (period) {
            StatsPeriod.MONTH -> Stats.inMonth(all, month)
            StatsPeriod.YEAR -> Stats.inYear(all, year)
            StatsPeriod.ALL -> all
        }
    }
    val totals = remember(periodMovements) { Stats.totals(periodMovements) }
    val chartEntries = remember(all, period, month, year, currentMonth) {
        when (period) {
            StatsPeriod.MONTH -> {
                val series = Stats.dailySeries(all, month)
                series.map { day ->
                    val d = day.date.dayOfMonth
                    val showLabel = d == 1 || d % 5 == 0 && d <= series.size - 2 || d == series.size
                    ChartEntry(
                        BarGroup(if (showLabel) d.toString() else "", day.totals.incomeCents, day.totals.expenseCents),
                        Dates.mediumDay(day.date),
                    )
                }
            }
            StatsPeriod.YEAR -> Stats.monthlySeries(all, year).map {
                ChartEntry(
                    BarGroup(Dates.shortMonth(it.month).take(1), it.totals.incomeCents, it.totals.expenseCents),
                    Dates.monthTitle(it.month),
                )
            }
            StatsPeriod.ALL -> Stats.lastMonthsSeries(all, currentMonth, 12).map {
                ChartEntry(
                    BarGroup(Dates.shortMonth(it.month).take(1), it.totals.incomeCents, it.totals.expenseCents),
                    Dates.monthTitle(it.month),
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "title") {
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge)
        }
        item(key = "period") {
            Column {
                val options = listOf(
                    StatsPeriod.MONTH to R.string.period_month,
                    StatsPeriod.YEAR to R.string.period_year,
                    StatsPeriod.ALL to R.string.period_all,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = period == value,
                            onClick = { period = value },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
                when (period) {
                    StatsPeriod.MONTH -> PeriodSwitcher(
                        title = Dates.monthTitle(month),
                        onPrevious = { monthText = month.minusMonths(1).toString() },
                        onNext = { monthText = month.plusMonths(1).toString() },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    StatsPeriod.YEAR -> PeriodSwitcher(
                        title = year.toString(),
                        onPrevious = { year -= 1 },
                        onNext = { year += 1 },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    StatsPeriod.ALL -> Unit
                }
            }
        }
        item(key = "summary") { SummaryCard(totals) }

        if (periodMovements.isEmpty() && period != StatsPeriod.ALL) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.BarChart,
                    title = stringResource(R.string.stats_no_data),
                    body = null,
                )
            }
        }

        if (all.isNotEmpty()) {
            item(key = "chart") {
                val title = when (period) {
                    StatsPeriod.MONTH -> R.string.stats_daily
                    StatsPeriod.YEAR -> R.string.stats_monthly
                    StatsPeriod.ALL -> R.string.stats_last_months
                }
                SectionCard(title = stringResource(title)) {
                    IncomeExpenseBarChart(
                        groups = chartEntries.map { it.bar },
                        selectedIndex = selectedBar,
                        onSelect = { selectedBar = it },
                        description = stringResource(title),
                    )
                    Spacer(Modifier.height(12.dp))
                    val selected = selectedBar?.let { chartEntries.getOrNull(it) }
                    if (selected != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                selected.detailTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                Money.format(selected.bar.incomeCents, signed = true),
                                color = AppTheme.money.income,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                Money.format(-selected.bar.expenseCents),
                                color = AppTheme.money.expense,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Legend()
                    }
                }
            }
        }

        if (periodMovements.isNotEmpty()) {
            val categories = Stats.byCategory(periodMovements)
            if (categories.isNotEmpty()) {
                item(key = "categories") { CategoryCard(categories, totals.expenseCents) }
            }
            item(key = "accounts") { AccountCard(periodMovements) }
            val members = Stats.byMember(periodMovements) { state.authorName(it) }
            if (state.isCloud && members.size > 1) {
                item(key = "members") { MembersCard(members) }
            }
            val incomeCategories = Stats.byCategory(periodMovements, income = true)
            if (incomeCategories.size > 1) {
                item(key = "incomeCategories") {
                    CategoryCard(incomeCategories, totals.incomeCents, title = R.string.stats_income_by_category)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(totals: Totals) {
    SectionCard {
        Row(Modifier.fillMaxWidth()) {
            SummaryValue(
                label = stringResource(R.string.stats_income),
                value = Money.format(totals.incomeCents),
                color = AppTheme.money.income,
                modifier = Modifier.weight(1f),
            )
            SummaryValue(
                label = stringResource(R.string.stats_expenses),
                value = Money.format(totals.expenseCents),
                color = AppTheme.money.expense,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.stats_net),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        Money.format(totals.netCents, signed = true),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totals.netCents >= 0) AppTheme.money.income else AppTheme.money.expense,
                    )
                }
                val message = when {
                    totals.incomeCents > 0 && totals.netCents >= 0 -> {
                        val rate = totals.netCents.toDouble() / totals.incomeCents
                        stringResource(R.string.stats_savings_rate, NumberFormat.getPercentInstance().format(rate))
                    }
                    totals.netCents < 0 && totals.incomeCents > 0 ->
                        stringResource(R.string.stats_overspent, Money.format(-totals.netCents))
                    else -> null
                }
                if (message != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Legend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        LegendItem(AppTheme.money.income, stringResource(R.string.stats_income))
        Spacer(Modifier.width(20.dp))
        LegendItem(AppTheme.money.expense, stringResource(R.string.stats_expenses))
    }
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.stats_tap_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CategoryCard(
    categories: List<com.chiiraac.migasto.domain.CategoryTotal>,
    totalCents: Long,
    title: Int = R.string.stats_by_category,
) {
    val percent = remember { NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 } }
    SectionCard(title = stringResource(title)) {
        DonutChart(
            slices = categories.map { DonutSlice(it.share, Categories.byId(it.categoryId).color) },
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            description = stringResource(title),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.stats_total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Money.format(totalCents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        categories.forEach { item ->
            val category = Categories.byId(item.categoryId)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(category.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(category.label),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    percent.format(item.share.toDouble()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    Money.format(item.cents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AccountCard(movements: List<Movement>) {
    val byAccount = remember(movements) {
        PaymentMethod.entries.associateWith { method ->
            movements.filter { it.type.isOutflow && it.method == method }.sumOf { it.amountCents }
        }
    }
    val total = byAccount.values.sum()
    if (total == 0L) return
    SectionCard(title = stringResource(R.string.stats_by_account)) {
        PaymentMethod.entries.forEach { method ->
            val cents = byAccount.getValue(method)
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(method.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(method.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(Money.format(cents), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            ShareBar(
                fraction = cents.toFloat() / total,
                color = if (method == PaymentMethod.BANK) AppTheme.money.bankDot else AppTheme.money.cashDot,
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MembersCard(members: List<com.chiiraac.migasto.domain.MemberTotal>) {
    val maxExpense = members.maxOf { it.totals.expenseCents }.coerceAtLeast(1)
    SectionCard(title = stringResource(R.string.stats_by_member)) {
        members.forEach { member ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(member.name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.stats_member_income, Money.format(member.totals.incomeCents)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    Money.format(member.totals.expenseCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.money.expense,
                )
            }
            ShareBar(member.totals.expenseCents.toFloat() / maxExpense, AppTheme.money.expense.copy(alpha = 0.8f))
            Spacer(Modifier.height(6.dp))
        }
    }
}
