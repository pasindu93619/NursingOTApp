// com/pasindu/nursingotapp/data/local/dao/ClaimPeriodDao.kt
package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimPeriodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaimPeriod(claimPeriod: ClaimPeriodEntity): Long

    @Query("SELECT * FROM claim_period ORDER BY createdAt DESC")
    fun observeClaimPeriods(): Flow<List<ClaimPeriodEntity>>

    @Query("SELECT * FROM claim_period WHERE id = :id LIMIT 1")
    suspend fun getClaimPeriodById(id: Long): ClaimPeriodEntity?

    // --- NEW: DELETE FUNCTIONS ---
    @Delete
    suspend fun deleteClaimPeriod(claimPeriod: ClaimPeriodEntity)

    @Query("DELETE FROM claim_period")
    suspend fun deleteAllClaimPeriods()
}