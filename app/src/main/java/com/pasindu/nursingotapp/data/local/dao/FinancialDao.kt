package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.FinancialRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(record: FinancialRecordEntity)

    // Retrieves all records for the Vico Charting historical graph
    @Query("SELECT * FROM financial_records ORDER BY timestamp DESC")
    fun getAllFinancialRecords(): Flow<List<FinancialRecordEntity>>

    // Uses 'recordMonth' matching the entity exactly (resolves 'monthYear' error)
    @Query("SELECT * FROM financial_records WHERE recordMonth = :month LIMIT 1")
    fun getFinancialRecordByMonth(month: String): Flow<FinancialRecordEntity?>

    @Delete
    suspend fun deleteFinancialRecord(record: FinancialRecordEntity)

    // Optional: Clears historical data if needed
    @Query("DELETE FROM financial_records")
    suspend fun clearAllFinancialRecords()
}