package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeReadingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingHistory(history: KnowledgeReadingHistoryEntity)

    @Update
    suspend fun updateReadingHistory(history: KnowledgeReadingHistoryEntity): Int

    @Query("SELECT * FROM knowledge_reading_history WHERE articleId = :articleId")
    suspend fun getReadingHistory(articleId: String): KnowledgeReadingHistoryEntity?

    @Query("SELECT * FROM knowledge_reading_history WHERE articleId = :articleId")
    fun observeReadingHistory(articleId: String): Flow<KnowledgeReadingHistoryEntity?>

    @Query("SELECT * FROM knowledge_reading_history WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun observeCompletedReadings(): Flow<List<KnowledgeReadingHistoryEntity>>

    @Query("DELETE FROM knowledge_reading_history WHERE articleId = :articleId")
    suspend fun deleteReadingHistory(articleId: String): Int
}
