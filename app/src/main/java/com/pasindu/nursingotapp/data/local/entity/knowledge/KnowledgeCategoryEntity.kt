package com.pasindu.nursingotapp.data.local.entity.knowledge

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_categories")
data class KnowledgeCategoryEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val displayOrder: Int = 0
)
