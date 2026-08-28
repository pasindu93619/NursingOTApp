package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryStep2027Dao {
    @Query("SELECT * FROM salary_steps_2027 WHERE grade = :grade ORDER BY salaryStep ASC")
    fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>>

    @Query("SELECT * FROM salary_steps_2027 WHERE grade = :grade AND salaryStep = :salaryStep LIMIT 1")
    suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity?

    @Query("DELETE FROM salary_steps_2027")
    suspend fun clearAll()

    @androidx.room.Insert
    suspend fun insertAll(rows: List<SalaryStep2027Entity>)
}
