package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferActiveCacheDao {

    @Upsert
    suspend fun upsert(cache: TransferActiveCacheEntity)

    @Query("SELECT * FROM transfer_active_cache WHERE id = 1 LIMIT 1")
    fun observe(): Flow<TransferActiveCacheEntity?>

    @Query("SELECT * FROM transfer_active_cache WHERE id = 1 LIMIT 1")
    suspend fun getOnce(): TransferActiveCacheEntity?

    @Query("DELETE FROM transfer_active_cache WHERE id = 1")
    suspend fun clear()
}
