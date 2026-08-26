package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicalPlanningDao {
    // ISBAR Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIsbarNote(note: IsbarNoteEntity)

    @Query("SELECT * FROM isbar_notes ORDER BY timestamp DESC")
    fun getAllIsbarNotes(): Flow<List<IsbarNoteEntity>>

    @Query("DELETE FROM isbar_notes WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNotes(cutoffTime: Long)

    // Clinical Tasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ClinicalTaskEntity)

    @Query("SELECT * FROM clinical_tasks ORDER BY triggerTime ASC")
    fun getAllTasks(): Flow<List<ClinicalTaskEntity>>

    @Query("UPDATE clinical_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Int, completed: Boolean)
}
