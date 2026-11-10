package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgePersonalNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: KnowledgePersonalNoteEntity): Long

    @Update
    suspend fun updateNote(note: KnowledgePersonalNoteEntity): Int

    @Query("DELETE FROM knowledge_personal_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long): Int

    @Query("SELECT * FROM knowledge_personal_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): KnowledgePersonalNoteEntity?

    @Query("SELECT * FROM knowledge_personal_notes WHERE articleId = :articleId ORDER BY updatedAt DESC")
    fun observeNotesForArticle(articleId: String): Flow<List<KnowledgePersonalNoteEntity>>

    @Query("SELECT * FROM knowledge_personal_notes WHERE originalArticleId = :originalArticleId ORDER BY updatedAt DESC")
    fun observeNotesByOriginalArticleId(originalArticleId: String): Flow<List<KnowledgePersonalNoteEntity>>

    @Query("SELECT * FROM knowledge_personal_notes ORDER BY updatedAt DESC")
    fun observeAllNotes(): Flow<List<KnowledgePersonalNoteEntity>>

    @Query("SELECT * FROM knowledge_personal_notes WHERE articleId = :articleId ORDER BY updatedAt DESC")
    suspend fun getNotesForArticle(articleId: String): List<KnowledgePersonalNoteEntity>
}
