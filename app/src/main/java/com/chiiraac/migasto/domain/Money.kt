package com.chiiraac.migasto.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/** Formato y lectura de importes. Internamente todo se maneja en céntimos (Long). */
object Money {
    val EUR: Currency = Currency.getInstance("EUR")

    /** Importe máximo aceptado en un movimiento: 9.999.999,99 €. */
    const val MAX_CENTS = 999_999_999L

    fun format(cents: Long, locale: Locale = Locale.getDefault(), signed: Boolean = false): String {
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = EUR
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val body = formatter.format(BigDecimal.valueOf(abs(cents), 2))
        return when {
            cents < 0 -> "-$body"
            signed && cents > 0 -> "+$body"
            else -> body
        }
    }

    /** Versión compacta para ejes de gráficas: "1,2 k€". */
    fun formatCompact(cents: Long, locale: Locale = Locale.getDefault()): String {
        val euros = cents / 100.0
        val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
        return when {
            abs(euros) >= 1_000_000 -> number.format(euros / 1_000_000) + " M€"
            abs(euros) >= 1_000 -> number.format(euros / 1_000) + " k€"
            else -> NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }.format(euros) + " €"
        }
    }

    /**
     * Convierte lo que escribe el usuario ("12", "12,5", "12.50", "1.234,56") en céntimos.
     * Devuelve null si no es un importe válido.
     */
    fun parseToCents(input: String): Long? {
        val cleaned = input.trim().replace(" ", "").replace("€", "")
        if (cleaned.isEmpty()) return null
        if (!cleaned.all { it.isDigit() || it == ',' || it == '.' }) return null

        val lastSeparator = cleaned.indexOfLast { it == ',' || it == '.' }
        val (integerPart, decimalPart) = if (lastSeparator >= 0) {
            val decimals = cleaned.substring(lastSeparator + 1)
            val integers = cleaned.substring(0, lastSeparator)
            if (decimals.length in 1..2 || (decimals.isEmpty() && integers.isNotEmpty())) {
                // El último separador es el decimal; el resto son separadores de miles.
                integers.filter { it.isDigit() } to decimals
            } else if (decimals.length == 3 && integers.isNotEmpty()) {
                // "1.234" → separador de miles
                (integers + decimals).filter { it.isDigit() } to ""
            } else {
                return null
            }
        } else {
            cleaned to ""
        }
        if (integerPart.isEmpty() && decimalPart.isEmpty()) return null
        if (integerPart.length > 10) return null
        val euros = integerPart.ifEmpty { "0" }.toLong()
        val cents = decimalPart.padEnd(2, '0').ifEmpty { "00" }.toLong()
        val total = euros * 100 + cents
        return total.takeIf { it in 0..MAX_CENTS }
    }

    /** Texto para rellenar el campo de importe al editar: 1250 → "12,50". */
    fun toInput(cents: Long, locale: Locale = Locale.getDefault()): String {
        val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val euros = cents / 100
        val rest = cents % 100
        return if (rest == 0L) euros.toString() else "$euros$separator${rest.toString().padStart(2, '0')}"
    }
}
