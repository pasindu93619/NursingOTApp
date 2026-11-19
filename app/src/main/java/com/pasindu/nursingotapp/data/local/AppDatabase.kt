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
import com.pasindu.nursingotapp.data.local.dao.ProfileAdditionalAllowanceDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDeductionDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.dao.PaySheetDocumentDao
import com.pasindu.nursingotapp.data.local.dao.TransferActiveCacheDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeArticleDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeBookmarkDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeNoteDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeOfflineCacheDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeReadingDao
import com.pasindu.nursingotapp.statgraph.data.local.StatDatasetDao
import com.pasindu.nursingotapp.statgraph.data.local.StatDatasetEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeSourceEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeCategoryEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeArticleEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeTagEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.ArticleTagCrossRef
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeBookmarkEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgePersonalNoteEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeReadingHistoryEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeOfflineCacheEntity
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.FinancialRecordEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileAdditionalAllowanceEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.data.local.entity.TransferActiveCacheEntity

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
        ProfileAdditionalAllowanceEntity::class,
        ProfileDeductionEntity::class,
        SalaryStep2027Entity::class,
        PaySheetDocumentEntity::class,
        TransferActiveCacheEntity::class,
        KnowledgeSourceEntity::class,
        KnowledgeCategoryEntity::class,
        KnowledgeArticleEntity::class,
        KnowledgeTagEntity::class,
        ArticleTagCrossRef::class,
        KnowledgeBookmarkEntity::class,
        KnowledgePersonalNoteEntity::class,
        KnowledgeReadingHistoryEntity::class,
        KnowledgeOfflineCacheEntity::class,
        StatDatasetEntity::class,
    ],
    version = 18,
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
    abstract fun knowledgeArticleDao(): KnowledgeArticleDao
    abstract fun knowledgeBookmarkDao(): KnowledgeBookmarkDao
    abstract fun knowledgeNoteDao(): KnowledgeNoteDao
    abstract fun knowledgeReadingDao(): KnowledgeReadingDao
    abstract fun knowledgeOfflineCacheDao(): KnowledgeOfflineCacheDao
    abstract fun payRateSettingsDao(): PayRateSettingsDao
    abstract fun profileCompensationDao(): ProfileCompensationDao
    abstract fun profileAdditionalAllowanceDao(): ProfileAdditionalAllowanceDao
    abstract fun profileDeductionDao(): ProfileDeductionDao
    abstract fun salaryStep2027Dao(): SalaryStep2027Dao
    abstract fun paySheetDocumentDao(): PaySheetDocumentDao
    abstract fun transferActiveCacheDao(): TransferActiveCacheDao
    abstract fun statDatasetDao(): StatDatasetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) = createSuperAppTables(database)
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // v2 and v3 have identical Room schemas. Preserve all user data
                // without recreating or replacing any tables.
            }
        }
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) = createSuperAppTables(database)
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val hasLegacyFinancialRecords = database.query(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='financial_records'"
                ).use { it.moveToFirst() }

                if (!hasLegacyFinancialRecords) {
                    // Some v3 databases were created without the legacy financial table.
                    // Create the v4 schema directly instead of attempting to rename a
                    // table that does not exist. No financial data can be preserved here
                    // because there is no source table to migrate from.
                    createFinancialRecordsTable(database)
                } else {
                    database.execSQL(
                        "ALTER TABLE `financial_records` RENAME TO `financial_records_legacy`"
                    )
                    createFinancialRecordsTable(database)

                    database.execSQL(
                        """
                        INSERT INTO `financial_records` (
                            `id`,
                            `recordMonth`,
                            `timestamp`,
                            `basicSalary`,
                            `otRate`,
                            `otHours`,
                            `phDays`,
                            `doDays`,
                            `wopDeduction`,
                            `apitTaxAmount`,
                            `loanDeduction`,
                            `otherDeductions`,
                            `totalHoursWorked`,
                            `grossSalary`,
                            `netSalary`
                        )
                        SELECT
                            `id`,
                            `monthYear`,
                            0,
                            `basicSalary`,
                            0,
                            0,
                            0,
                            0,
                            `wopPensionDeduction`,
                            `apitTaxDeduction`,
                            `loanDeduction`,
                            0,
                            0,
                            `basicSalary` + `totalAllowance` + `calculatedOtAmount`,
                            `netSalary`
                        FROM `financial_records_legacy`
                        """.trimIndent()
                    )

                    database.execSQL("DROP TABLE `financial_records_legacy`")
                }

                // Repair malformed zero-column tables produced by older Super App
                // database creation paths. Only a table with zero columns is replaced;
                // valid tables and all valid user data are preserved.
                repairZeroColumnTable(
                    database = database,
                    tableName = "isbar_notes",
                    createTable = ::createIsbarNotesTable
                )
                repairZeroColumnTable(
                    database = database,
                    tableName = "clinical_tasks",
                    createTable = ::createClinicalTasksTable
                )
                repairZeroColumnTable(
                    database = database,
                    tableName = "cpd_logs",
                    createTable = ::createCpdLogsTable
                )
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
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // v10 and v11 have the same persisted tables relevant to this migration.
                // Preserve existing salary data without creating untracked tables.
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
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `profile_additional_allowances` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `profile_deductions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `claimPeriodId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transfer_active_cache` (
                        `id` INTEGER NOT NULL,
                        `requestId` TEXT,
                        `requestStatus` TEXT,
                        `currentHospitalId` TEXT,
                        `preferenceHospitalIdsJson` TEXT,
                        `matchCycleId` TEXT,
                        `matchType` TEXT,
                        `matchStatus` TEXT,
                        `matchPayloadJson` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `transfer_active_cache` ADD COLUMN `grade` TEXT")
            }
        }


        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. knowledge_sources
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_sources` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `shortCode` TEXT NOT NULL,
                        `trustTier` TEXT NOT NULL,
                        `websiteUrl` TEXT,
                        `isSystemDefault` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 2. knowledge_categories
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_categories` (
                        `id` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 3. knowledge_tags
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_tags` (
                        `tag` TEXT NOT NULL,
                        PRIMARY KEY(`tag`)
                    )
                """.trimIndent())

                // 4. knowledge_articles
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_articles` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `sourceInstitution` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `clinicalSummary` TEXT NOT NULL,
                        `recommendationsJson` TEXT NOT NULL,
                        `evidenceGrade` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `lastReviewed` TEXT NOT NULL,
                        `sourceUrl` TEXT,
                        `circularNumber` TEXT,
                        `effectiveDate` TEXT,
                        `isFullTextAvailable` INTEGER NOT NULL,
                        `licenseNotice` TEXT,
                        `sourceUrlDisclaimer` TEXT,
                        `isSuperseded` INTEGER NOT NULL,
                        `supersededByArticleId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sourceInstitution`) REFERENCES `knowledge_sources`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_articles_sourceInstitution` ON `knowledge_articles` (`sourceInstitution`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_articles_category` ON `knowledge_articles` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_articles_evidenceGrade` ON `knowledge_articles` (`evidenceGrade`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_articles_year` ON `knowledge_articles` (`year`)")

                // 5. article_tag_cross_ref
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `article_tag_cross_ref` (
                        `articleId` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        PRIMARY KEY(`articleId`, `tag`),
                        FOREIGN KEY(`articleId`) REFERENCES `knowledge_articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`tag`) REFERENCES `knowledge_tags`(`tag`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_article_tag_cross_ref_tag` ON `article_tag_cross_ref` (`tag`)")

                // 6. knowledge_bookmarks
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_bookmarks` (
                        `articleId` TEXT NOT NULL,
                        `bookmarkedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`articleId`),
                        FOREIGN KEY(`articleId`) REFERENCES `knowledge_articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // 7. knowledge_personal_notes
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_personal_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `articleId` TEXT,
                        `originalArticleId` TEXT NOT NULL,
                        `noteContent` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`articleId`) REFERENCES `knowledge_articles`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_personal_notes_articleId` ON `knowledge_personal_notes` (`articleId`)")

                // 8. knowledge_reading_history
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_reading_history` (
                        `articleId` TEXT NOT NULL,
                        `lastReadAt` INTEGER NOT NULL,
                        `readingProgressPercent` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `linkedCpdLogId` INTEGER,
                        PRIMARY KEY(`articleId`),
                        FOREIGN KEY(`articleId`) REFERENCES `knowledge_articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`linkedCpdLogId`) REFERENCES `cpd_logs`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_reading_history_linkedCpdLogId` ON `knowledge_reading_history` (`linkedCpdLogId`)")

                // 9. knowledge_offline_cache
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `knowledge_offline_cache` (
                        `articleId` TEXT NOT NULL,
                        `localRelativePath` TEXT,
                        `mimeType` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `sha256Hash` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        `lastAccessedAt` INTEGER NOT NULL,
                        `isFullTextCached` INTEGER NOT NULL,
                        PRIMARY KEY(`articleId`),
                        FOREIGN KEY(`articleId`) REFERENCES `knowledge_articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_knowledge_offline_cache_localRelativePath` ON `knowledge_offline_cache` (`localRelativePath`)")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stat_datasets` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `sourceLabel` TEXT NOT NULL,
                        `schemaVersion` INTEGER NOT NULL,
                        `datasetPayloadJson` TEXT NOT NULL,
                        `mappingSelectionJson` TEXT NOT NULL,
                        `activeWorkspaceSection` TEXT NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stat_datasets_updatedAtEpochMillis` ON `stat_datasets` (`updatedAtEpochMillis`)"
                )
            }
        }

        private fun createSuperAppTables(database: SupportSQLiteDatabase) {
            createFinancialRecordsTable(database)
            createIsbarNotesTable(database)
            createClinicalTasksTable(database)
            createCpdLogsTable(database)
        }

        private fun repairZeroColumnTable(
            database: SupportSQLiteDatabase,
            tableName: String,
            createTable: (SupportSQLiteDatabase) -> Unit
        ) {
            val columnCount = database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                var count = 0
                while (cursor.moveToNext()) count++
                count
            }

            if (columnCount == 0) {
                database.execSQL("DROP TABLE IF EXISTS `$tableName`")
                createTable(database)
            }
        }

        private fun createIsbarNotesTable(database: SupportSQLiteDatabase) {
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
        }

        private fun createClinicalTasksTable(database: SupportSQLiteDatabase) {
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
        }

        private fun createCpdLogsTable(database: SupportSQLiteDatabase) {
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
