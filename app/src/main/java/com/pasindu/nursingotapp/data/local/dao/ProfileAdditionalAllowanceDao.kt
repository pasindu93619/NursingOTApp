package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.nursingotapp.data.local.entity.ProfileAdditionalAllowanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileAdditionalAllowanceDao {
    @Query("SELECT * FROM profile_additional_allowances ORDER BY id")
    fun observeAll(): Flow<List<ProfileAdditionalAllowanceEntity>>

    @Query("DELETE FROM profile_additional_allowances")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(items: List<ProfileAdditionalAllowanceEntity>)
}
