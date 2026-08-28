package com.pasindu.nursingotapp.data.paysheet

import android.net.Uri
import com.pasindu.nursingotapp.data.local.dao.PaySheetDocumentDao
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaySheetVaultRepository(
    private val dao: PaySheetDocumentDao,
    private val vault: PaySheetVaultManager
) {
    fun observeDocuments(): Flow<List<PaySheetDocumentEntity>> = dao.observeAll()

    suspend fun importPaysheet(sourceUri: Uri, monthKey: String): PaySheetDocumentEntity = withContext(Dispatchers.IO) {
        val safeMonth = normalizeMonthKey(monthKey)
        val existing = dao.findByMonth(safeMonth)

        val temp = File.createTempFile("paysheet_import_", ".jpg", vaultTempDir())
        try {
            val resolver = vault.contextResolver
            resolver.openInputStream(sourceUri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Unable to read the selected paysheet.")

            val target = vault.prepareInput(temp, safeMonth)
            val hash = vault.sha256(target)

            if (existing != null && existing.filePath != target.absolutePath) {
                vault.deleteFile(existing.filePath)
            }

            val now = System.currentTimeMillis()
            val document = PaySheetDocumentEntity(
                id = existing?.id ?: 0L,
                monthKey = safeMonth,
                displayMonth = displayMonth(safeMonth),
                filePath = target.absolutePath,
                fileSizeBytes = vault.fileSize(target.absolutePath),
                sha256 = hash,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            dao.upsert(document)
            document
        } finally {
            temp.delete()
        }
    }

    suspend fun delete(document: PaySheetDocumentEntity) = withContext(Dispatchers.IO) {
        dao.delete(document)
        vault.deleteFile(document.filePath)
    }

    suspend fun findByMonth(monthKey: String): PaySheetDocumentEntity? = dao.findByMonth(normalizeMonthKey(monthKey))

    fun storageBytes(): Long = vault.totalBytes()

    private fun normalizeMonthKey(value: String): String {
        require(value.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) { "Month must use yyyy-MM format." }
        return value
    }

    private fun displayMonth(value: String): String {
        val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(value) ?: return value
        return DateFormatSymbols(Locale.US).months[SimpleDateFormat("MM", Locale.US).format(date).toInt() - 1]
            .replaceFirstChar { it.titlecase(Locale.US) } + " " + SimpleDateFormat("yyyy", Locale.US).format(date)
    }

    private fun vaultTempDir(): File = File(vault.rootDirectory, ".tmp").also { it.mkdirs() }
}
