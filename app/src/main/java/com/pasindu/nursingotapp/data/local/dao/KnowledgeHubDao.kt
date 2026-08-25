package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeHubDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCpdLog(log: CpdLogEntity)

    @Query("SELECT * FROM cpd_logs ORDER BY date DESC")
    fun getAllCpdLogs(): Flow<List<CpdLogEntity>>
}