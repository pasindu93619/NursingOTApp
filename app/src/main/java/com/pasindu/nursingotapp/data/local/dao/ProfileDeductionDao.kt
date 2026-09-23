package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDeductionDao {
    @Query("SELECT * FROM profile_deductions WHERE claimPeriodId = :claimPeriodId ORDER BY id")
    fun observeForClaimPeriod(claimPeriodId: Long): Flow<List<ProfileDeductionEntity>>

    @Query("DELETE FROM profile_deductions WHERE claimPeriodId = :claimPeriodId")
    suspend fun deleteForClaimPeriod(claimPeriodId: Long)

    @Upsert
    suspend fun upsertAll(items: List<ProfileDeductionEntity>)
}
