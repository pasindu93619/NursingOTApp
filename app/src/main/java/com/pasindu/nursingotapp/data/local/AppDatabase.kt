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
        ProfileEntity::class,
        ClaimPeriodEntity::class,
        DailyEntryEntity::class,
        FinancialRecordEntity::class,
        IsbarNoteEntity::class,
        ClinicalTaskEntity::class,
        CpdLogEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun claimPeriodDao(): ClaimPeriodDao
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun financialDao(): FinancialDao
    abstract fun clinicalPlanningDao(): ClinicalPlanningDao
    abstract fun knowledgeHubDao(): KnowledgeHubDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createSuperAppTables(database)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createSuperAppTables(database)
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createSuperAppTables(database)
            }
        }

        // Version 3 -> 4: replace the legacy financial_records schema
        // with the current monthly financial model used by FinancialRecordEntity.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `financial_records`")
                createFinancialRecordsTable(database)
            }
        }

        private fun createSuperAppTables(database: SupportSQLiteDatabase) {
            createFinancialRecordsTable(database)

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

        private fun createFinancialRecordsTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `financial_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `recordMonth` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `basicSalary` REAL NOT NULL,
                    `otRate` REAL NOT NULL,
                    `otHours` REAL NOT NULL,
                    `phDays` REAL NOT NULL,
                    `doDays` REAL NOT NULL,
                    `wopDeduction` REAL NOT NULL,
                    `apitTaxAmount` REAL NOT NULL,
                    `loanDeduction` REAL NOT NULL,
                    `otherDeductions` REAL NOT NULL,
                    `totalHoursWorked` REAL NOT NULL,
                    `grossSalary` REAL NOT NULL,
                    `netSalary` REAL NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}
