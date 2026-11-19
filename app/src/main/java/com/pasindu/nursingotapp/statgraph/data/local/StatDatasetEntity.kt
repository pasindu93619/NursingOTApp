package com.pasindu.nursingotapp.statgraph.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity persisting a Stat Graph Studio aggregate dataset along with its
 * active chart mapping selection and workspace workflow section.
 *
 * Uses a stable UUID primary key ([id]) rather than mutable dataset names so
 * renames and timestamp updates never orphan or collide with saved records.
 */
@Entity(
    tableName = "stat_datasets",
    indices = [
        Index(value = ["updatedAtEpochMillis"])
    ]
)
data class StatDatasetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sourceLabel: String,
    val schemaVersion: Int,
    val datasetPayloadJson: String,
    val mappingSelectionJson: String,
    val activeWorkspaceSection: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
