package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeOfflineCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeOfflineCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCacheEntry(entry: KnowledgeOfflineCacheEntity)

    @Update
    suspend fun updateCacheEntry(entry: KnowledgeOfflineCacheEntity): Int

    @Query("SELECT * FROM knowledge_offline_cache WHERE articleId = :articleId")
    suspend fun getByArticleId(articleId: String): KnowledgeOfflineCacheEntity?

    @Query("SELECT * FROM knowledge_offline_cache WHERE articleId = :articleId")
    fun observeByArticleId(articleId: String): Flow<KnowledgeOfflineCacheEntity?>

    @Query("SELECT * FROM knowledge_offline_cache WHERE localRelativePath = :relativePath LIMIT 1")
    suspend fun getByPath(relativePath: String): KnowledgeOfflineCacheEntity?

    @Query("SELECT localRelativePath FROM knowledge_offline_cache WHERE localRelativePath IS NOT NULL")
    suspend fun getAllActivePaths(): List<String>

    @Query("SELECT * FROM knowledge_offline_cache WHERE isFullTextCached = 1")
    suspend fun getAllFullTextCached(): List<KnowledgeOfflineCacheEntity>

    @Query("UPDATE knowledge_offline_cache SET localRelativePath = :relativePath WHERE articleId = :articleId")
    suspend fun updateRelativePath(articleId: String, relativePath: String?): Int

    @Query("UPDATE knowledge_offline_cache SET isFullTextCached = :isFullTextCached, localRelativePath = :path WHERE articleId = :articleId")
    suspend fun updateCacheStatus(articleId: String, isFullTextCached: Int, path: String?): Int

    @Query("DELETE FROM knowledge_offline_cache WHERE articleId = :articleId")
    suspend fun deleteByArticleId(articleId: String): Int
}
