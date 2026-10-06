package com.chiiraac.migasto.domain

import com.chiiraac.migasto.data.model.Movement
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Genera un CSV compatible con Excel en español: separador ";" y coma decimal.
 * Se incluye BOM UTF-8 para que los acentos se vean bien al abrirlo.
 */
object CsvExporter {

    data class Labels(
        val header: List<String>,
        val typeName: (Movement) -> String,
        val categoryName: (Movement) -> String,
        val methodName: (Movement) -> String,
        val authorName: (Movement) -> String,
    )

    private const val SEPARATOR = ';'
    private const val BOM = "\uFEFF"
    private val NUMBER = Regex("-?\\d+([.,]\\d+)?")

    fun build(movements: List<Movement>, labels: Labels, locale: Locale = Locale.getDefault()): String {
        val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val amountFormat = DecimalFormat("0.00", symbols)
        val separator = if (symbols.decimalSeparator == ',') SEPARATOR else ','
        val sb = StringBuilder(BOM)
        sb.appendRow(labels.header, separator)
        movements.sortedWith(compareBy<Movement> { it.date }.thenBy { it.createdAt }).forEach { m ->
            sb.appendRow(
                listOf(
                    dateFormat.format(m.date),
                    labels.typeName(m),
                    labels.categoryName(m),
                    m.description,
                    labels.methodName(m),
                    amountFormat.format(BigDecimal.valueOf(m.signedAmountForCsv(), 2)),
                    labels.authorName(m),
                ),
                separator,
            )
        }
        return sb.toString()
    }

    private fun Movement.signedAmountForCsv(): Long = if (signedAmount == 0L) amountCents else signedAmount

    private fun StringBuilder.appendRow(values: List<String>, separator: Char) {
        values.forEachIndexed { index, value ->
            if (index > 0) append(separator)
            append(escape(value, separator))
        }
        append("\r\n")
    }

    internal fun escape(value: String, separator: Char = SEPARATOR): String {
        // Evita inyección de fórmulas al abrir el CSV en una hoja de cálculo
        // (los importes negativos son números y se dejan tal cual).
        val isNumber = NUMBER.matches(value)
        val safe = if (!isNumber && value.isNotEmpty() && value.first() in "=+-@\t\r") "'$value" else value
        val needsQuotes = safe.any { it == separator || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
