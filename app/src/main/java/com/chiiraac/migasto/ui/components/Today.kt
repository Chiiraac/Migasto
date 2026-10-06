package com.chiiraac.migasto.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/** Permite fijar la fecha de "hoy" (útil en pruebas y capturas). */
val LocalToday = compositionLocalOf<LocalDate?> { null }

/** Fecha actual; se actualiza sola al pasar la medianoche. */
@Composable
fun today(): LocalDate {
    LocalToday.current?.let { return it }
    var date by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(date) {
        val untilMidnight = Duration.between(LocalDateTime.now(), date.plusDays(1).atStartOfDay())
        delay(untilMidnight.toMillis().coerceAtLeast(1_000L))
        date = LocalDate.now()
    }
    return date
}
