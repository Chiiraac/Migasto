package com.chiiraac.migasto.data.local

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Guarda las fotos de los tickets como JPEG en el almacenamiento interno de la app. */
class LocalPhotoStore(private val directory: File) {

    private fun fileFor(movementId: String) = File(directory, "$movementId.jpg")

    suspend fun save(movementId: String, jpegBytes: ByteArray) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val target = fileFor(movementId)
        val temp = File(directory, "$movementId.tmp")
        temp.writeBytes(jpegBytes)
        if (!temp.renameTo(target)) {
            target.writeBytes(jpegBytes)
            temp.delete()
        }
    }

    suspend fun load(movementId: String): ByteArray? = withContext(Dispatchers.IO) {
        fileFor(movementId).takeIf { it.exists() }?.readBytes()
    }

    suspend fun delete(movementId: String) = withContext(Dispatchers.IO) {
        fileFor(movementId).delete()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        directory.deleteRecursively()
    }
}
