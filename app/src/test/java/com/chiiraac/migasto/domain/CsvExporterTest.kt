package com.chiiraac.migasto.domain

import com.chiiraac.migasto.TestData
import com.chiiraac.migasto.data.model.MovementType
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    private val labels = CsvExporter.Labels(
        header = listOf("Fecha", "Tipo", "Categoría", "Descripción", "Cuenta", "Importe", "Autor"),
        typeName = { it.type.name },
        categoryName = { it.categoryId },
        methodName = { it.method.name },
        authorName = { it.createdByName },
    )

    @Test
    fun `builds spanish excel friendly csv`() {
        val movements = listOf(
            TestData.movement("Pan; leche", 250, MovementType.EXPENSE, "groceries", LocalDate.of(2026, 10, 2)),
            TestData.movement("=SUMA(A1)", 100_000, MovementType.INCOME, "salary", LocalDate.of(2026, 10, 1)),
        )
        val csv = CsvExporter.build(movements, labels, Locale.forLanguageTag("es-ES"))
        val lines = csv.removePrefix("\uFEFF").trimEnd().lines()
        assertEquals(3, lines.size)
        assertEquals("Fecha;Tipo;Categoría;Descripción;Cuenta;Importe;Autor", lines[0])
        assertTrue(lines[1], lines[1].startsWith("2026-10-01;INCOME;salary;'=SUMA(A1);BANK;1000,00;"))
        assertTrue(lines[2], lines[2].contains("\"Pan; leche\";BANK;-2,50;"))
    }

    @Test
    fun `amounts use ascii digits and minus sign in any locale`() {
        val movements = listOf(TestData.movement("Pan", 250, MovementType.EXPENSE, "groceries", LocalDate.of(2026, 10, 2)))
        val finnish = CsvExporter.build(movements, labels, Locale.forLanguageTag("fi-FI"))
        assertTrue(finnish, finnish.contains(";-2,50;"))
        val english = CsvExporter.build(movements, labels, Locale.US)
        assertTrue(english, english.contains(",-2.50,"))
    }

    @Test
    fun `escapes quotes`() {
        assertEquals("\"Dice \"\"hola\"\"\"", CsvExporter.escape("Dice \"hola\""))
        assertEquals("-12,50", CsvExporter.escape("-12,50"))
        assertEquals("'+34", CsvExporter.escape("+34"))
    }
}
