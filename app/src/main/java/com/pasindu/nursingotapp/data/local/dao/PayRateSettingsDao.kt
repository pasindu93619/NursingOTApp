package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayRateSettingsDao {
    @Query("SELECT * FROM pay_rate_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<PayRateSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: PayRateSettingsEntity)
}
