package com.pasindu.nursingotapp.data.local.entity.knowledge

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_sources")
data class KnowledgeSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val shortCode: String,
    val trustTier: String,
    val websiteUrl: String?,
    val isSystemDefault: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
