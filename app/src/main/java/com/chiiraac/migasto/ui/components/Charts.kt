package com.chiiraac.migasto.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiiraac.migasto.domain.Money
import com.chiiraac.migasto.ui.theme.AppTheme
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Immutable
data class BarGroup(
    /** Etiqueta del eje X (vacía para no mostrarla). */
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long,
)

/** Redondea hacia arriba a un valor "bonito" (1, 2, 2.5, 5 × 10^n) para la escala. */
internal fun niceCeiling(value: Long): Long {
    if (value <= 0) return 100
    val exponent = floorLog10(value.toDouble())
    val base = 10.0.pow(exponent)
    val fraction = value / base
    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return ceil(nice * base).toLong()
}

private fun floorLog10(value: Double): Int = kotlin.math.floor(log10(value)).toInt()

/**
 * Gráfico de barras agrupadas (ingresos en verde, gastos en rojo) con escala en el eje Y.
 * Al tocar un grupo se notifica su índice para mostrar el detalle.
 */
@Composable
fun IncomeExpenseBarChart(
    groups: List<BarGroup>,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    description: String = "",
) {
    val income = AppTheme.money.income
    val expense = AppTheme.money.expense
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)

    val maxValue = remember(groups) {
        niceCeiling(groups.maxOfOrNull { max(it.incomeCents, it.expenseCents) } ?: 0L)
    }
    val yLabels = remember(maxValue) {
        listOf(0L, maxValue / 2, maxValue).map { Money.formatCompact(it) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(groups, selectedIndex) {
                detectTapGestures { offset ->
                    if (groups.isEmpty()) return@detectTapGestures
                    val left = yAxisWidthPx(yLabels, textMeasurer, labelStyle)
                    val slot = (size.width - left) / groups.size
                    val index = ((offset.x - left) / slot).toInt()
                    if (index in groups.indices) onSelect(if (index == selectedIndex) null else index) else onSelect(null)
                }
            },
    ) {
        val axisWidth = yAxisWidthPx(yLabels, textMeasurer, labelStyle)
        val bottomLabelHeight = 22.dp.toPx()
        val topPadding = 8.dp.toPx()
        val chartLeft = axisWidth
        val chartBottom = size.height - bottomLabelHeight
        val chartHeight = chartBottom - topPadding
        val chartWidth = size.width - chartLeft

        // Líneas de referencia y etiquetas del eje Y
        yLabels.forEachIndexed { i, label ->
            val y = chartBottom - chartHeight * (i / 2f)
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (i == 0) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            val layout = textMeasurer.measure(label, labelStyle)
            drawText(
                layout,
                topLeft = Offset(
                    x = axisWidth - layout.size.width - 6.dp.toPx(),
                    y = (y - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height),
                ),
            )
        }

        if (groups.isEmpty()) return@Canvas
        val slot = chartWidth / groups.size
        val barWidth = min(slot * 0.4f, 16.dp.toPx())
        val gap = min(2.dp.toPx(), slot * 0.06f)
        val radius = CornerRadius(min(barWidth / 2f, 4.dp.toPx()))

        groups.forEachIndexed { index, group ->
            val slotLeft = chartLeft + slot * index
            val center = slotLeft + slot / 2f
            if (index == selectedIndex) {
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset(slotLeft + slot * 0.08f, topPadding),
                    size = Size(slot * 0.84f, chartHeight),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
            fun bar(value: Long, x: Float, color: Color) {
                if (value <= 0) return
                val h = max(chartHeight * (value.toFloat() / maxValue), 2.dp.toPx())
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, chartBottom - h),
                    size = Size(barWidth, h),
                    cornerRadius = radius,
                )
            }
            bar(group.incomeCents, center - gap / 2f - barWidth, income)
            bar(group.expenseCents, center + gap / 2f, expense)

            if (group.label.isNotEmpty()) {
                val layout = textMeasurer.measure(group.label, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = (center - layout.size.width / 2f).coerceIn(chartLeft, size.width - layout.size.width),
                        y = chartBottom + 6.dp.toPx(),
                    ),
                )
            }
        }
    }
}

private fun androidx.compose.ui.unit.Density.yAxisWidthPx(
    labels: List<String>,
    measurer: androidx.compose.ui.text.TextMeasurer,
    style: TextStyle,
): Float = labels.maxOf { measurer.measure(it, style).size.width } + 10.dp.toPx()

@Immutable
data class DonutSlice(val fraction: Float, val color: Color)

/** Gráfico de anillo. [center] se dibuja en el hueco central. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    thickness: Dp = 26.dp,
    description: String = "",
    center: @Composable BoxScope.() -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = thickness.toPx()
            val diameter = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val gapDegrees = if (slices.size > 1) 1.5f else 0f
            var start = -90f
            slices.forEach { slice ->
                val sweep = slice.fraction * 360f
                if (sweep > gapDegrees) {
                    drawArc(
                        color = slice.color,
                        startAngle = start + gapDegrees / 2f,
                        sweepAngle = sweep - gapDegrees,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                }
                start += sweep
            }
        }
        center()
    }
}

/** Barra horizontal de proporción (para miembros y cuentas). */
@Composable
fun ShareBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
