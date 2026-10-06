package com.chiiraac.migasto.ui.home

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.domain.Money
import com.chiiraac.migasto.domain.Stats
import com.chiiraac.migasto.ui.components.BalanceCard
import com.chiiraac.migasto.ui.components.EmptyState
import com.chiiraac.migasto.ui.components.MovementRow
import com.chiiraac.migasto.ui.components.today
import com.chiiraac.migasto.ui.main.MainUiState
import com.chiiraac.migasto.ui.theme.AppTheme
import java.time.YearMonth

@Composable
fun HomeScreen(
    state: MainUiState,
    onMovementClick: (Movement) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val movements = state.movements
    val balance = remember(movements) { Stats.balance(movements) }
    val currentMonth = YearMonth.from(today())
    val monthTotals = remember(movements, currentMonth) { Stats.totals(Stats.inMonth(movements, currentMonth)) }

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
        item(key = "balance") { BalanceCard(balance) }
        item(key = "month") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonthPill(
                    label = stringResource(R.string.home_month_income),
                    amount = Money.format(monthTotals.incomeCents),
                    color = AppTheme.money.income,
                    icon = Icons.Rounded.SouthWest,
                    modifier = Modifier.weight(1f),
                )
                MonthPill(
                    label = stringResource(R.string.home_month_expenses),
                    amount = Money.format(monthTotals.expenseCents),
                    color = AppTheme.money.expense,
                    icon = Icons.Rounded.NorthEast,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "title") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.recent_transactions),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (movements.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(R.plurals.movement_count, movements.size, movements.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        when {
            !state.movementsLoaded -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            movements.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                )
            }
            else -> items(movements, key = { it.id }) { movement ->
                MovementRow(
                    movement = movement,
                    authorName = if (state.isCloud) state.authorName(movement) else null,
                    onClick = { onMovementClick(movement) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun MonthPill(label: String, amount: String, color: Color, icon: ImageVector, modifier: Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.16f), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                BasicText(
                    amount,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp),
                )
            }
        }
    }
}
