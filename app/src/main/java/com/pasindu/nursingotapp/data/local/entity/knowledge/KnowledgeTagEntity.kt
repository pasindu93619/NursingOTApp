package com.pasindu.nursingotapp.data.local.entity.knowledge

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_tags")
data class KnowledgeTagEntity(
    @PrimaryKey
    val tag: String
)
