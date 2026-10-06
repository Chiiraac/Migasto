package com.chiiraac.migasto.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.domain.CsvExporter
import com.chiiraac.migasto.ui.components.Categories
import com.chiiraac.migasto.ui.components.label
import com.chiiraac.migasto.ui.components.transferLabel
import com.chiiraac.migasto.util.TempFiles
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Genera el CSV de un grupo y abre el menú de compartir de Android. */
object CsvShare {

    suspend fun share(
        context: Context,
        group: Group,
        movements: List<Movement>,
        authorName: (Movement) -> String,
    ) {
        val labels = CsvExporter.Labels(
            header = listOf(
                R.string.csv_date, R.string.csv_type, R.string.csv_category, R.string.csv_description,
                R.string.csv_account, R.string.csv_amount, R.string.csv_author,
            ).map(context::getString),
            typeName = { context.getString(it.type.label) },
            categoryName = { context.getString(Categories.byId(it.categoryId).label) },
            methodName = {
                context.getString(if (it.type == MovementType.TRANSFER) it.method.transferLabel else it.method.label)
            },
            authorName = authorName,
        )
        val file = withContext(Dispatchers.IO) {
            val directory = TempFiles.exportsDir(context).apply {
                deleteRecursively()
                mkdirs()
            }
            val safeName = group.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').ifEmpty { "grupo" }
            File(directory, "MiGasto_${safeName}_${LocalDate.now()}.csv").apply {
                writeText(CsvExporter.build(movements, labels), Charsets.UTF_8)
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject, group.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.export_chooser))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }
}
