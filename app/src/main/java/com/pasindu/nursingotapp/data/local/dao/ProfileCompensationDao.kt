package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileCompensationDao {
    @Upsert
    suspend fun upsert(compensation: ProfileCompensationEntity)

    @Query("SELECT * FROM profile_compensation WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ProfileCompensationEntity?>

    @Query("SELECT * FROM profile_compensation WHERE id = 1 LIMIT 1")
    suspend fun getOnce(): ProfileCompensationEntity?
}
