package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.FinancialRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(
        record: FinancialRecordEntity
    )

    @Query(
        "SELECT * FROM financial_records ORDER BY id DESC"
    )
    fun getAllFinancialRecords():
            Flow<List<FinancialRecordEntity>>

    @Query(
        "SELECT * FROM financial_records " +
                "WHERE monthYear = :monthYear " +
                "LIMIT 1"
    )
    suspend fun getRecordByMonth(
        monthYear: String
    ): FinancialRecordEntity?
}