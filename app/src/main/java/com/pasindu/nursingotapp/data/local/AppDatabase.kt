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
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.dao.PaySheetDocumentDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.FinancialRecordEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity

@Database(
    entities = [
        ProfileEntity::class,
        ClaimPeriodEntity::class,
        DailyEntryEntity::class,
        FinancialRecordEntity::class,
        IsbarNoteEntity::class,
        ClinicalTaskEntity::class,
        CpdLogEntity::class,
        PayRateSettingsEntity::class,
        ProfileCompensationEntity::class,
        SalaryStep2027Entity::class,
        PaySheetDocumentEntity::class
    ],
    version = 12,
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
    abstract fun payRateSettingsDao(): PayRateSettingsDao
    abstract fun profileCompensationDao(): ProfileCompensationDao
    abstract fun salaryStep2027Dao(): SalaryStep2027Dao
    abstract fun paySheetDocumentDao(): PaySheetDocumentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) = createSuperAppTables(database)
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) = createSuperAppTables(database)
        }
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) = createSuperAppTables(database)
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `financial_records`")
                createFinancialRecordsTable(database)
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pay_rate_settings` (
                        `id` INTEGER NOT NULL,
                        `otRate` REAL NOT NULL,
                        `phRate` REAL NOT NULL,
                        `doRate` REAL NOT NULL,
                        `rateSource` TEXT NOT NULL,
                        `basisSalary2027` REAL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `profile_compensation` (
                        `id` INTEGER NOT NULL,
                        `riskAllowance` REAL NOT NULL,
                        `claAllowance` REAL NOT NULL,
                        `additionalAllowancesTotal` REAL NOT NULL,
                        `totalDeductions` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `salary_steps_2027` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `grade` TEXT NOT NULL,
                        `salaryStep` INTEGER NOT NULL,
                        `currentBasicSalary2026` REAL NOT NULL,
                        `basicSalary2027` REAL NOT NULL,
                        `effectiveFrom` TEXT NOT NULL,
                        `sourceLabel` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `profile` ADD COLUMN `salaryStep` INTEGER")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("PRAGMA table_info(`salary_steps_2027`)")
                var exists = false
                cursor.use {
                    val nameIndex = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        if (nameIndex >= 0 && it.getString(nameIndex) == "currentBasicSalary2026") {
                            exists = true
                            break
                        }
                    }
                }
                if (!exists) {
                    database.execSQL("ALTER TABLE `salary_steps_2027` ADD COLUMN `currentBasicSalary2026` REAL NOT NULL DEFAULT 0.0")
                }
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `salary_steps_2027`")
                database.execSQL("""
                    CREATE TABLE `salary_steps_2027` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `grade` TEXT NOT NULL,
                        `salaryStep` INTEGER NOT NULL,
                        `currentBasicSalary2026` REAL NOT NULL,
                        `basicSalary2027` REAL NOT NULL,
                        `effectiveFrom` TEXT NOT NULL,
                        `sourceLabel` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `salary_table_meta` (`id` INTEGER NOT NULL PRIMARY KEY, `datasetVersion` TEXT NOT NULL)")
                database.execSQL("DELETE FROM `salary_table_meta`")
                database.execSQL("INSERT INTO `salary_table_meta` (`id`, `datasetVersion`) VALUES (1, '2026-2027-supplied-paid-amounts-v1')")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pay_sheet_documents` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `monthKey` TEXT NOT NULL,
                        `displayMonth` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `sha256` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pay_sheet_documents_monthKey` ON `pay_sheet_documents` (`monthKey`)")
            }
        }

        private fun createSuperAppTables(database: SupportSQLiteDatabase) {
            createFinancialRecordsTable(database)
            database.execSQL("""
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
            """.trimIndent())
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `clinical_tasks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `taskName` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `triggerTime` INTEGER NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `bypassDnd` INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `cpd_logs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `seminarTitle` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `earnedPoints` INTEGER NOT NULL,
                    `speakerOrInstitution` TEXT NOT NULL,
                    `notes` TEXT NOT NULL
                )
            """.trimIndent())
        }

        private fun createFinancialRecordsTable(database: SupportSQLiteDatabase) {
            database.execSQL("""
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
            """.trimIndent())
        }
    }
}
