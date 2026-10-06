package com.chiiraac.migasto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.domain.Balance
import com.chiiraac.migasto.domain.Dates
import com.chiiraac.migasto.domain.Money
import com.chiiraac.migasto.domain.signedAmount
import com.chiiraac.migasto.ui.theme.AppTheme
import com.chiiraac.migasto.ui.theme.BalanceTextStyle

/** Cuadrado redondeado con el icono de la categoría, teñido con el color del tipo. */
@Composable
fun CategoryBadge(
    type: MovementType,
    categoryId: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val category = Categories.byId(categoryId)
    val tint = type.color()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = stringResource(category.label),
            tint = tint,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

/** Importe con signo y color según el tipo de movimiento. */
@Composable
fun movementAmountText(movement: Movement): String =
    if (movement.type == MovementType.TRANSFER) {
        Money.format(movement.amountCents)
    } else {
        Money.format(movement.signedAmount, signed = true)
    }

@Composable
fun movementTitle(movement: Movement): String =
    movement.description.ifBlank { stringResource(Categories.byId(movement.categoryId).label) }

/**
 * Tarjeta de un movimiento en las listas. A diferencia del diseño original, los textos
 * largos se recortan con "…" en lugar de partirse letra a letra.
 */
@Composable
fun MovementRow(
    movement: Movement,
    authorName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = Categories.byId(movement.categoryId)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(movement.type, movement.categoryId)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = movementTitle(movement),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (movement.hasPhoto) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.AttachFile,
                                contentDescription = stringResource(R.string.cd_has_photo),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = movementAmountText(movement),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = movement.type.color(),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val meta = stringResource(category.label) + " • " + Dates.shortDay(movement.date)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        // La categoría y la fecha tienen prioridad; el autor se recorta primero.
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!authorName.isNullOrBlank()) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = authorName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    MethodIcons(movement)
                }
            }
        }
    }
}

/** Icono de la cuenta (o "banco → efectivo" en los traspasos). */
@Composable
fun MethodIcons(movement: Movement, modifier: Modifier = Modifier) {
    val description = stringResource(
        if (movement.type == MovementType.TRANSFER) movement.method.transferLabel else movement.method.label,
    )
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(movement.method.icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        if (movement.type == MovementType.TRANSFER) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp),
            )
            Icon(movement.method.other.icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

/** Tarjeta morada con el saldo total y el desglose banco / efectivo. */
@Composable
fun BalanceCard(balance: Balance, modifier: Modifier = Modifier) {
    val money = AppTheme.money
    val totalLabel = stringResource(R.string.balance_total)
    val totalText = Money.format(balance.totalCents)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(money.balanceCard, money.balanceCardEnd)))
            .padding(horizontal = 28.dp, vertical = 28.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$totalLabel: $totalText" },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = money.onBalanceCard.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = totalText,
                style = BalanceTextStyle.copy(color = money.onBalanceCard, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = BalanceTextStyle.fontSize),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AccountAmount(
                    label = stringResource(R.string.method_bank),
                    amount = Money.format(balance.bankCents),
                    icon = PaymentMethod.BANK.icon,
                    dot = money.bankDot,
                    alignEnd = false,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                AccountAmount(
                    label = stringResource(R.string.method_cash),
                    amount = Money.format(balance.cashCents),
                    icon = PaymentMethod.CASH.icon,
                    dot = money.cashDot,
                    alignEnd = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AccountAmount(
    label: String,
    amount: String,
    icon: ImageVector,
    dot: Color,
    alignEnd: Boolean,
    modifier: Modifier,
) {
    val color = AppTheme.money.onBalanceCard
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = dot, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = color.copy(alpha = 0.9f))
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = amount,
            style = MaterialTheme.typography.titleLarge.copy(color = color, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 22.sp),
        )
    }
}

/** Selector "< Octubre 2026 >". */
@Composable
fun PeriodSwitcher(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = nextEnabled) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next))
        }
    }
}

/** Tarjeta con título para agrupar secciones (Ajustes, Estadísticas…). */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

/** Mensaje centrado con icono para listas vacías. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Círculo con la inicial de una persona. */
@Composable
fun MemberAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val palette = listOf(
        Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899),
        Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF14B8A6),
    )
    val color = palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Fila clicable de ajustes con icono, título y subtítulo. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
