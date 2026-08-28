package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryStep2027Dao {

    @Query("SELECT * FROM salary_steps_2027 WHERE grade = :grade ORDER BY salaryStep ASC")
    fun observeForGrade(grade: String): Flow<List<SalaryStep2027Entity>>

    @Query("SELECT * FROM salary_steps_2027 WHERE grade = :grade AND salaryStep = :salaryStep LIMIT 1")
    suspend fun find(grade: String, salaryStep: Int): SalaryStep2027Entity?

    /** Match a nurse's current basic salary to the supplied grade table. */
    @Query("SELECT * FROM salary_steps_2027 WHERE grade = :grade AND ABS(basicSalary2027 - :currentBasicSalary) < 0.01 LIMIT 1")
    suspend fun findByCurrentBasic(grade: String, currentBasicSalary: Double): SalaryStep2027Entity?

    @Query("SELECT COUNT(*) FROM salary_steps_2027")
    suspend fun count(): Int

    @Query("DELETE FROM salary_steps_2027")
    suspend fun clearAll()

    @Insert
    suspend fun insertAll(rows: List<SalaryStep2027Entity>)
}
