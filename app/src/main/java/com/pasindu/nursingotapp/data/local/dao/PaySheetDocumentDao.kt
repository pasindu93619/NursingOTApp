package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaySheetDocumentDao {
    @Query("SELECT * FROM pay_sheet_documents ORDER BY monthKey DESC")
    fun observeAll(): Flow<List<PaySheetDocumentEntity>>

    @Query("SELECT * FROM pay_sheet_documents WHERE monthKey = :monthKey LIMIT 1")
    suspend fun findByMonth(monthKey: String): PaySheetDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: PaySheetDocumentEntity): Long

    @Delete
    suspend fun delete(document: PaySheetDocumentEntity)

    @Query("DELETE FROM pay_sheet_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}
