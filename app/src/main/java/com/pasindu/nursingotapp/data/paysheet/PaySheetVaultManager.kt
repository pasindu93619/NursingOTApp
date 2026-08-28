package com.pasindu.nursingotapp.data.paysheet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Private, app-scoped storage for monthly paysheet images.
 * Images are never stored inside Room and are not written to public media storage.
 */
class PaySheetVaultManager(private val context: Context) {

    private val rootDir: File
        get() = File(context.filesDir, "paysheet_vault")

    fun vaultFile(monthKey: String): File {
        val safeKey = monthKey.replace("[^0-9-]".toRegex(), "_")
        val year = safeKey.take(4).ifBlank { "unknown" }
        val month = safeKey.drop(5).take(2).ifBlank { "00" }
        return File(File(File(rootDir, year), month), "paysheet_$safeKey.jpg")
    }

    fun prepareInput(source: File, monthKey: String, maxDimension: Int = 2400, quality: Int = 88): File {
        require(source.exists()) { "Paysheet image does not exist." }
        val bitmap = BitmapFactory.decodeFile(source.absolutePath)
            ?: error("Unable to read paysheet image.")

        val scaled = scaleDown(bitmap, maxDimension)
        val target = vaultFile(monthKey)
        target.parentFile?.mkdirs()
        target.outputStream().use { output ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                "Unable to store paysheet image."
            }
        }

        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return target
    }

    fun deleteFile(path: String): Boolean = File(path).delete()

    fun exists(path: String): Boolean = File(path).exists()

    fun fileSize(path: String): Long = File(path).takeIf { it.exists() }?.length() ?: 0L

    fun sha256(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(File(path)).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun totalBytes(): Long = rootDir.walkTopDown()
        .filter { it.isFile }
        .sumOf { it.length() }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
