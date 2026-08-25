package com.pasindu.nursingotapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.FinancialDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeHubDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.FinancialRecordEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity

@Database(
    entities = [
        // Legacy Core Entities
        ProfileEntity::class,
        ClaimPeriodEntity::class,
        DailyEntryEntity::class,
        // Super App Entities
        FinancialRecordEntity::class,
        IsbarNoteEntity::class,
        ClinicalTaskEntity::class,
        CpdLogEntity::class
    ],
    version = 3, // Incremented to 3 to resolve identity hash mismatch
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Legacy DAOs
    abstract fun profileDao(): ProfileDao
    abstract fun claimPeriodDao(): ClaimPeriodDao
    abstract fun dailyEntryDao(): DailyEntryDao

    // Super App DAOs
    abstract fun financialDao(): FinancialDao
    abstract fun clinicalPlanningDao(): ClinicalPlanningDao
    abstract fun knowledgeHubDao(): KnowledgeHubDao

    companion object {
        // Migration from Version 1 to Version 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createSuperAppTables(database)
            }
        }

        // Migration from Version 2 to Version 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createSuperAppTables(database)
            }
        }

        // Direct Migration from Version 1 to Version 3
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createSuperAppTables(database)
            }
        }

        private fun createSuperAppTables(database: SupportSQLiteDatabase) {
            // 1. Financial Records Table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `financial_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `monthYear` TEXT NOT NULL,
                    `basicSalary` REAL NOT NULL,
                    `totalAllowance` REAL NOT NULL,
                    `calculatedOtAmount` REAL NOT NULL,
                    `apitTaxDeduction` REAL NOT NULL,
                    `wopPensionDeduction` REAL NOT NULL,
                    `loanDeduction` REAL NOT NULL,
                    `netSalary` REAL NOT NULL
                )
                """.trimIndent()
            )

            // 2. ISBAR Notes Table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `isbar_notes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `patientIdentifier` TEXT NOT NULL,
                    `identification` TEXT NOT NULL,
                    `situation` TEXT NOT NULL,
                    `background` TEXT NOT NULL,
                    `assessment` TEXT NOT NULL,
                    `recommendation` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 3. Clinical Tasks Table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `clinical_tasks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `taskName` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `triggerTime` INTEGER NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `bypassDnd` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 4. CPD Logs Table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cpd_logs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `seminarTitle` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `earnedPoints` INTEGER NOT NULL,
                    `speakerOrInstitution` TEXT NOT NULL,
                    `notes` TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}