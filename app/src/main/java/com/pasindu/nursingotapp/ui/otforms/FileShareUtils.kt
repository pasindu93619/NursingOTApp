// com/pasindu/nursingotapp/ui/otforms/FileShareUtils.kt
package com.pasindu.nursingotapp.ui.otforms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileShareUtils {

    /** Shares the PDF through the Android system share sheet without copying the file first. */
    fun sharePdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share OT Claim PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to open sharing options.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves a PDF into Downloads/NursingOT.
     * Returns the public content Uri when available so the caller can provide an
     * immediate Open/Share action without guessing the physical file location.
     */
    fun savePdfToDownloads(context: Context, file: File): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/NursingOT"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return null

                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(file).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: throw IllegalStateException("Unable to open destination stream")

                    val completedValues = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    resolver.update(uri, completedValues, null, null)
                    uri
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val nursingDir = File(downloadsDir, "NursingOT")
                if (!nursingDir.exists() && !nursingDir.mkdirs()) {
                    throw IllegalStateException("Unable to create NursingOT download folder")
                }

                val destFile = File(nursingDir, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF to phone.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun showSavedToast(context: Context) {
        Toast.makeText(
            context,
            "PDF saved to Downloads/NursingOT",
            Toast.LENGTH_LONG
        ).show()
    }
}
