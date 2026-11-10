package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: KnowledgeBookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmarksOrIgnore(bookmarks: List<KnowledgeBookmarkEntity>)

    @Query("DELETE FROM knowledge_bookmarks WHERE articleId = :articleId")
    suspend fun deleteBookmark(articleId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM knowledge_bookmarks WHERE articleId = :articleId)")
    fun observeIsBookmarked(articleId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM knowledge_bookmarks WHERE articleId = :articleId)")
    suspend fun isBookmarked(articleId: String): Boolean

    @Query("SELECT articleId FROM knowledge_bookmarks ORDER BY bookmarkedAt DESC")
    fun observeAllBookmarkedArticleIds(): Flow<List<String>>

    @Query("SELECT articleId FROM knowledge_bookmarks ORDER BY bookmarkedAt DESC")
    suspend fun getAllBookmarkedArticleIds(): List<String>

    @Query("SELECT * FROM knowledge_bookmarks WHERE articleId = :articleId")
    suspend fun getBookmarkByArticleId(articleId: String): KnowledgeBookmarkEntity?
}
