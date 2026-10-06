package com.chiiraac.migasto.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reduce y comprime las fotos de tickets para que ocupen poco (< 700 KB) y
 * se puedan sincronizar rápido. Respeta la orientación EXIF de la cámara.
 */
object PhotoProcessor {
    private const val MAX_DIMENSION = 1600
    private const val MAX_BYTES = 700 * 1024

    suspend fun compress(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Imagen no válida")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("No se pudo leer la imagen")

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { exifRotation(ExifInterface(it)) }
        }.getOrNull() ?: 0

        var bitmap = transform(decoded, MAX_DIMENSION, rotation)
        var quality = 85
        var bytes = encode(bitmap, quality)
        var attempts = 0
        while (bytes.size > MAX_BYTES && attempts < MAX_ATTEMPTS) {
            if (quality > 50) {
                quality -= 10
            } else {
                val smaller = (max(bitmap.width, bitmap.height) * 0.75f).toInt()
                bitmap = transform(bitmap, smaller, 0)
                quality = 75
            }
            bytes = encode(bitmap, quality)
            attempts++
        }
        bitmap.recycle()
        bytes
    }

    private const val MAX_ATTEMPTS = 12

    private fun encode(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    private fun transform(source: Bitmap, maxDimension: Int, rotation: Int): Bitmap {
        val scale = min(1f, maxDimension.toFloat() / max(source.width, source.height))
        if (scale >= 1f && rotation == 0) return source
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (result !== source) source.recycle()
        return result
    }

    private fun exifRotation(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
}
