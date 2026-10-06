package com.chiiraac.migasto.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ficheros temporales de la app: fotos originales de la cámara (antes de comprimirlas)
 * y CSV exportados. Se borran en cuanto dejan de hacer falta.
 */
object TempFiles {
    const val CAMERA_DIR = "camera"
    const val EXPORTS_DIR = "exports"

    fun cameraDir(context: Context) = File(context.cacheDir, CAMERA_DIR)
    fun exportsDir(context: Context) = File(context.cacheDir, EXPORTS_DIR)

    /** Borra la foto original si [uri] apunta a una captura de la cámara guardada por la app. */
    suspend fun deleteCameraCapture(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        if (uri.authority != "${context.packageName}.fileprovider") return@withContext
        val name = uri.lastPathSegment ?: return@withContext
        File(cameraDir(context), File(name).name).delete()
    }

    /** Borra todas las capturas y exportaciones (al borrar la cuenta o cerrar sesión). */
    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        cameraDir(context).deleteRecursively()
        exportsDir(context).deleteRecursively()
    }

    /** Borra capturas antiguas que quedaron de fotos canceladas o sin guardar. */
    suspend fun pruneStale(context: Context, maxAgeHours: Long = 24) = withContext(Dispatchers.IO) {
        val limit = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(maxAgeHours)
        cameraDir(context).listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
        exportsDir(context).listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
    }
}
