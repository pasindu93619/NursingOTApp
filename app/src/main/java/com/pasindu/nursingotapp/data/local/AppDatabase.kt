package com.pasindu.nursingotapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pasindu.nursingotapp.data.local.dao.*
import com.pasindu.nursingotapp.data.local.entity.*

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
    version = 2,
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
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `financial_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `monthYear` TEXT NOT NULL, `basicSalary` REAL NOT NULL, `totalAllowance` REAL NOT NULL, `calculatedOtAmount` REAL NOT NULL, `apitTaxDeduction` REAL NOT NULL, `wopPensionDeduction` REAL NOT NULL, `loanDeduction` REAL NOT NULL, `netSalary` REAL NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `isbar_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientIdentifier` TEXT NOT NULL, `identification` TEXT NOT NULL, `situation` TEXT NOT NULL, `background` TEXT NOT NULL, `assessment` TEXT NOT NULL, `recommendation` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `clinical_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskName` TEXT NOT NULL, `description` TEXT NOT NULL, `priority` TEXT NOT NULL, `triggerTime` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `bypassDnd` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `cpd_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `seminarTitle` TEXT NOT NULL, `date` INTEGER NOT NULL, `earnedPoints` INTEGER NOT NULL, `speakerOrInstitution` TEXT NOT NULL, `notes` TEXT NOT NULL)")
            }
        }
    }
}