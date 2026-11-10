package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeArticleEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeCategoryEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeSourceEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeTagEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.ArticleTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeArticleDao {
    @Upsert
    suspend fun upsertArticle(article: KnowledgeArticleEntity)

    @Upsert
    suspend fun upsertArticles(articles: List<KnowledgeArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: KnowledgeArticleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<KnowledgeArticleEntity>)

    @Update
    suspend fun updateArticle(article: KnowledgeArticleEntity)

    @Query("SELECT * FROM knowledge_articles WHERE id = :id")
    suspend fun getArticleById(id: String): KnowledgeArticleEntity?

    @Query("SELECT * FROM knowledge_articles WHERE id = :id")
    fun observeArticleById(id: String): Flow<KnowledgeArticleEntity?>

    @Query("SELECT * FROM knowledge_articles ORDER BY year DESC, title ASC")
    fun observeAllArticles(): Flow<List<KnowledgeArticleEntity>>

    @Query("SELECT * FROM knowledge_articles ORDER BY year DESC, title ASC")
    suspend fun getAllArticles(): List<KnowledgeArticleEntity>

    @Query("SELECT * FROM knowledge_articles WHERE category = :category ORDER BY year DESC, title ASC")
    fun observeArticlesByCategory(category: String): Flow<List<KnowledgeArticleEntity>>

    @Query("SELECT id FROM knowledge_articles WHERE id IN (:ids)")
    suspend fun getExistingArticleIds(ids: List<String>): List<String>

    @Query("DELETE FROM knowledge_articles WHERE id = :id")
    suspend fun deleteArticleById(id: String): Int

    // Sources
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: KnowledgeSourceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSources(sources: List<KnowledgeSourceEntity>)

    @Query("SELECT * FROM knowledge_sources ORDER BY name ASC")
    fun observeAllSources(): Flow<List<KnowledgeSourceEntity>>

    // Categories
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: KnowledgeCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<KnowledgeCategoryEntity>)

    @Query("SELECT * FROM knowledge_categories ORDER BY displayOrder ASC, displayName ASC")
    fun observeAllCategories(): Flow<List<KnowledgeCategoryEntity>>

    // Tags & CrossRef
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: KnowledgeTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<KnowledgeTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticleTagCrossRef(crossRef: ArticleTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticleTagCrossRefs(crossRefs: List<ArticleTagCrossRef>)

    @Query("SELECT tag FROM article_tag_cross_ref WHERE articleId = :articleId")
    fun observeTagsForArticle(articleId: String): Flow<List<String>>
}
