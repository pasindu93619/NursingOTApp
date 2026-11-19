package com.pasindu.nursingotapp.statgraph.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for saved Stat Graph Studio datasets.
 *
 * Deliberately omits an unguarded bulk-wipe method to prevent accidental
 * destructive deletion of saved statistical datasets.
 */
@Dao
interface StatDatasetDao {

    @Query("SELECT * FROM stat_datasets ORDER BY updatedAtEpochMillis DESC")
    fun observeAllOrderedByUpdatedDesc(): Flow<List<StatDatasetEntity>>

    @Query("SELECT * FROM stat_datasets ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    fun observeMostRecentlyUpdated(): Flow<StatDatasetEntity?>

    @Query("SELECT * FROM stat_datasets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StatDatasetEntity?

    @Query("SELECT * FROM stat_datasets ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun getMostRecentlyUpdatedOnce(): StatDatasetEntity?

    @Upsert
    suspend fun upsert(entity: StatDatasetEntity)

    @Query("DELETE FROM stat_datasets WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
