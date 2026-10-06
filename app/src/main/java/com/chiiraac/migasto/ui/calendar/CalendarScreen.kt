package com.chiiraac.migasto.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.domain.DayActivity
import com.chiiraac.migasto.domain.Dates
import com.chiiraac.migasto.domain.Money
import com.chiiraac.migasto.domain.Stats
import com.chiiraac.migasto.ui.components.MovementRow
import com.chiiraac.migasto.ui.components.PeriodSwitcher
import com.chiiraac.migasto.ui.components.today
import com.chiiraac.migasto.ui.main.MainUiState
import com.chiiraac.migasto.ui.theme.AppTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    state: MainUiState,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onMovementClick: (Movement) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val todayDate = today()
    val visibleMonth = YearMonth.from(selectedDate)
    val activity = remember(state.movements, visibleMonth) { Stats.activityByDay(state.movements, visibleMonth) }
    val dayMovements = remember(state.movements, selectedDate) { Stats.onDay(state.movements, selectedDate) }
    val dayTotals = remember(dayMovements) { Stats.totals(dayMovements) }

    fun goToMonth(month: YearMonth) {
        onSelectDate(if (month == YearMonth.from(todayDate)) todayDate else month.atDay(1))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.calendar_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                if (selectedDate != todayDate) {
                    TextButton(onClick = { onSelectDate(todayDate) }) { Text(stringResource(R.string.calendar_today)) }
                }
            }
        }
        item(key = "grid") {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    PeriodSwitcher(
                        title = Dates.monthTitle(visibleMonth),
                        onPrevious = { goToMonth(visibleMonth.minusMonths(1)) },
                        onNext = { goToMonth(visibleMonth.plusMonths(1)) },
                    )
                    MonthGrid(
                        month = visibleMonth,
                        selected = selectedDate,
                        today = todayDate,
                        activity = activity,
                        onSelect = onSelectDate,
                    )
                }
            }
        }
        item(key = "dayTitle") {
            Column(Modifier.padding(top = 8.dp)) {
                Text(
                    stringResource(R.string.calendar_day_title, selectedDate.dayOfMonth),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (dayTotals.incomeCents > 0 || dayTotals.expenseCents > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (dayTotals.incomeCents > 0) {
                            Text(
                                Money.format(dayTotals.incomeCents, signed = true),
                                color = AppTheme.money.income,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (dayTotals.expenseCents > 0) {
                            Text(
                                Money.format(-dayTotals.expenseCents),
                                color = AppTheme.money.expense,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        if (dayMovements.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.calendar_empty_day),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(dayMovements, key = { it.id }) { movement ->
                MovementRow(
                    movement = movement,
                    authorName = if (state.isCloud) state.authorName(movement) else null,
                    onClick = { onMovementClick(movement) },
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    activity: Map<Int, DayActivity>,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDay = remember { Dates.firstDayOfWeek() }
    val cells = remember(month, firstDay) { Dates.monthGrid(month, firstDay) }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Dates.weekDays(firstDay).forEach { day ->
                Text(
                    text = Dates.weekDayInitial(day),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isSelected = date == selected,
                                isToday = date == today,
                                activity = activity[date.dayOfMonth],
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    activity: DayActivity?,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val description = Dates.longDay(date)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.background(primary) else Modifier)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, primary, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
        if (activity != null) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (activity.hasIncome) Dot(AppTheme.money.income)
                if (activity.hasExpense) Dot(AppTheme.money.expense)
                if (activity.hasTransfer && !activity.hasIncome && !activity.hasExpense) Dot(AppTheme.money.transfer)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color),
    )
}
