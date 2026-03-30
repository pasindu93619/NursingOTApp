// com/pasindu/nursingotapp/data/local/dao/DailyEntryDao.kt
package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEntryDao {

    // RESTORED: Needed by NursingViewModel
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DailyEntryEntity)

    // RESTORED: Needed by NursingViewModel
    @Update
    suspend fun updateEntry(entry: DailyEntryEntity)

    @Upsert
    suspend fun upsertDailyEntry(entry: DailyEntryEntity)

    // RESTORED: Needed by NursingViewModel
    @Query("SELECT * FROM daily_entry WHERE claimPeriodId = :claimPeriodId ORDER BY date ASC")
    fun observeEntriesForPeriod(claimPeriodId: Long): Flow<List<DailyEntryEntity>>

    // NEW: Used for the Smart Analytics Screen to see trends over time
    @Query("SELECT * FROM daily_entry ORDER BY date ASC")
    fun observeAllEntries(): Flow<List<DailyEntryEntity>>

    @Query("SELECT * FROM daily_entry WHERE claimPeriodId = :claimPeriodId AND date = :date LIMIT 1")
    suspend fun getEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity?

    @Query("DELETE FROM daily_entry WHERE claimPeriodId = :claimPeriodId")
    suspend fun deleteEntriesForPeriod(claimPeriodId: Long)

    @Query("DELETE FROM daily_entry")
    suspend fun deleteAllEntries()
}