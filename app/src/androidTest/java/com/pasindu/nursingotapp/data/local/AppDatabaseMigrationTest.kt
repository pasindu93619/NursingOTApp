package com.pasindu.nursingotapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import android.database.sqlite.SQLiteConstraintException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private lateinit var helper: MigrationTestHelper

    @Before
    fun setUp() {
        helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java
        )
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate3To4PreservesFinancialRecords() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO financial_records (
                    id, monthYear, basicSalary, totalAllowance, calculatedOtAmount,
                    apitTaxDeduction, wopPensionDeduction, loanDeduction, netSalary
                ) VALUES (17, '2026-08', 120000.0, 15000.0, 4500.0, 1200.0, 9000.0, 2500.0, 136800.0)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            AppDatabase.MIGRATION_3_4
        ).use { db ->
            db.query(
                """
                SELECT id,
                       recordMonth,
                       basicSalary,
                       otRate,
                       otHours,
                       phDays,
                       doDays,
                       wopDeduction,
                       apitTaxAmount,
                       loanDeduction,
                       otherDeductions,
                       totalHoursWorked,
                       grossSalary,
                       netSalary
                FROM financial_records
                WHERE id = 17
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(17, cursor.getInt(0))
                assertEquals("2026-08", cursor.getString(1))
                assertEquals(120000.0, cursor.getDouble(2), 0.0)

                // v3 did not store these new decomposed input fields.
                // Do not invent an OT rate/hours or PH/DO values during migration.
                assertEquals(0.0, cursor.getDouble(3), 0.0)
                assertEquals(0.0, cursor.getDouble(4), 0.0)
                assertEquals(0.0, cursor.getDouble(5), 0.0)
                assertEquals(0.0, cursor.getDouble(6), 0.0)

                assertEquals(9000.0, cursor.getDouble(7), 0.0)
                assertEquals(1200.0, cursor.getDouble(8), 0.0)
                assertEquals(2500.0, cursor.getDouble(9), 0.0)
                assertEquals(0.0, cursor.getDouble(10), 0.0)
                assertEquals(0.0, cursor.getDouble(11), 0.0)

                // v3 stored allowance and calculated OT as components but v4 stores
                // a single gross amount. Preserve their aggregate using the v4 gross model.
                assertEquals(139500.0, cursor.getDouble(12), 0.0)
                assertEquals(136800.0, cursor.getDouble(13), 0.0)
            }
        }
    }
    @Test
    fun migrate2To3PreservesLegacyFinancialRecords() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
            INSERT INTO financial_records (
                id,
                monthYear,
                basicSalary,
                totalAllowance,
                calculatedOtAmount,
                apitTaxDeduction,
                wopPensionDeduction,
                loanDeduction,
                netSalary
            ) VALUES (
                41,
                '2026-07',
                115000.0,
                12000.0,
                3500.0,
                1000.0,
                8500.0,
                2000.0,
                119000.0
            )
            """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            AppDatabase.MIGRATION_2_3
        ).use { db ->
            db.query(
                """
            SELECT
                id,
                monthYear,
                basicSalary,
                totalAllowance,
                calculatedOtAmount,
                apitTaxDeduction,
                wopPensionDeduction,
                loanDeduction,
                netSalary
            FROM financial_records
            WHERE id = 41
            """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(41, cursor.getInt(0))
                assertEquals("2026-07", cursor.getString(1))
                assertEquals(115000.0, cursor.getDouble(2), 0.0)
                assertEquals(12000.0, cursor.getDouble(3), 0.0)
                assertEquals(3500.0, cursor.getDouble(4), 0.0)
                assertEquals(1000.0, cursor.getDouble(5), 0.0)
                assertEquals(8500.0, cursor.getDouble(6), 0.0)
                assertEquals(2000.0, cursor.getDouble(7), 0.0)
                assertEquals(119000.0, cursor.getDouble(8), 0.0)
            }
        }
    }
    @Test
    fun migrate6To7PreservesExistingProfileDataAndCreatesSalaryTable() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """
            INSERT INTO profile (
                id, fullName, serviceNo, unit, paySheetNo,
                grade, basicSalary, otRate, updatedAt
            ) VALUES (
                1, 'Salary Migration Nurse', 'S-200', 'Ward 3',
                'PS-200', 'MN 3', 120000.0, 283.0, 1710000000000
            )
            """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            AppDatabase.MIGRATION_6_7
        ).use { db ->
            db.query(
                "SELECT fullName, serviceNo, basicSalary, otRate FROM profile WHERE id = 1"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("Salary Migration Nurse", cursor.getString(0))
                assertEquals("S-200", cursor.getString(1))
                assertEquals(120000.0, cursor.getDouble(2), 0.0)
                assertEquals(283.0, cursor.getDouble(3), 0.0)
            }

            db.query(
                """
    SELECT grade,
           salaryStep,
           basicSalary2027,
           effectiveFrom,
           sourceLabel
    FROM salary_steps_2027
    """.trimIndent()
            ).use { cursor ->
                assertEquals(0, cursor.count)
            }
        }
    }

    @Test
    fun migrate7To8PreservesProfileDataAndAddsSalaryStep() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                """
            INSERT INTO profile (
                id, fullName, serviceNo, unit, paySheetNo,
                grade, basicSalary, otRate, updatedAt
            ) VALUES (
                2, 'Profile Migration Nurse', 'S-201', 'Ward 4',
                'PS-201', 'MN 3', 125000.0, 283.0, 1710000000000
            )
            """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            8,
            true,
            AppDatabase.MIGRATION_7_8
        ).use { db ->
            db.query(
                "SELECT fullName, serviceNo, basicSalary, otRate, salaryStep FROM profile WHERE id = 2"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("Profile Migration Nurse", cursor.getString(0))
                assertEquals("S-201", cursor.getString(1))
                assertEquals(125000.0, cursor.getDouble(2), 0.0)
                assertEquals(283.0, cursor.getDouble(3), 0.0)
                assertEquals(0, cursor.getInt(4))
            }
        }
    }
    @Test
    fun migrate8To9PreservesSalarySteps() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                """
                INSERT INTO salary_steps_2027 (
                    id, grade, salaryStep, basicSalary2027, effectiveFrom, sourceLabel
                ) VALUES (23, 'MN 3', 5, 71500.0, '2027-01-01', 'migration-test')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        ).use { db ->
            db.query(
                "SELECT grade, salaryStep, currentBasicSalary2026, basicSalary2027 FROM salary_steps_2027 WHERE id = 23"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("MN 3", cursor.getString(0))
                assertEquals(5, cursor.getInt(1))
                assertEquals(0.0, cursor.getDouble(2), 0.0)
                assertEquals(71500.0, cursor.getDouble(3), 0.0)
            }
        }
    }

    @Test
    fun migrate9To10PreservesSalarySteps() {
        helper.createDatabase(TEST_DB, 9).apply {
            execSQL(
                """
                INSERT INTO salary_steps_2027 (
                    id, grade, salaryStep, currentBasicSalary2026, basicSalary2027, effectiveFrom, sourceLabel
                ) VALUES (31, 'MN 3', 6, 68000.0, 75000.0, '2027-01-01', 'migration-test')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            10,
            true,
            AppDatabase.MIGRATION_9_10
        ).use { db ->
            db.query(
                "SELECT grade, salaryStep, currentBasicSalary2026, basicSalary2027, sourceLabel FROM salary_steps_2027 WHERE id = 31"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("MN 3", cursor.getString(0))
                assertEquals(6, cursor.getInt(1))
                assertEquals(68000.0, cursor.getDouble(2), 0.0)
                assertEquals(75000.0, cursor.getDouble(3), 0.0)
                assertEquals("migration-test", cursor.getString(4))
            }
        }
    }

    @Test
    fun migrate11To12CreatesPaySheetDocumentsAndPreservesProfile() {
        helper.createDatabase(TEST_DB, 11).apply {
            execSQL(
                """
                INSERT INTO profile (
                    id, fullName, serviceNo, unit, paySheetNo, grade, basicSalary, otRate, updatedAt, salaryStep
                ) VALUES (1, 'Migration Nurse', 'S-001', 'Ward 1', 'PS-001', 'MN 3', 120000.0, 283.0, 1710000000000, 5)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            AppDatabase.MIGRATION_11_12
        ).use { db ->
            db.query("SELECT fullName, salaryStep FROM profile WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("Migration Nurse", cursor.getString(0))
                assertEquals(5, cursor.getInt(1))
            }

            db.execSQL(
                """
                INSERT INTO pay_sheet_documents (
                    monthKey, displayMonth, filePath, fileSizeBytes, sha256, createdAt, updatedAt
                ) VALUES ('2026-08', 'August 2026', '/data/pay-sheet.pdf', 1024, 'abc123', 1710000000000, 1710000005000)
                """.trimIndent()
            )

            db.query("SELECT monthKey, sha256 FROM pay_sheet_documents WHERE monthKey = '2026-08'").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("2026-08", cursor.getString(0))
                assertEquals("abc123", cursor.getString(1))
            }
        }
    }
    @Test
    fun migrate4To5PreservesFinancialRecordsAndCreatesPayRateSettings() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                """
            INSERT INTO financial_records (
                id, recordMonth, timestamp, basicSalary, otRate, otHours,
                phDays, doDays, wopDeduction, apitTaxAmount, loanDeduction,
                otherDeductions, totalHoursWorked, grossSalary, netSalary
            ) VALUES (
                1, '2026-08', 1710000000000, 120000.0, 283.0, 12.0,
                2.0, 1.0, 9000.0, 1200.0, 2500.0,
                500.0, 36.0, 139500.0, 136300.0
            )
            """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            AppDatabase.MIGRATION_4_5
        ).use { db ->

            db.query(
                """
            SELECT recordMonth, basicSalary, otRate, otHours,
                   phDays, doDays, grossSalary, netSalary
            FROM financial_records
            WHERE id = 1
            """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())

                assertEquals("2026-08", cursor.getString(0))
                assertEquals(120000.0, cursor.getDouble(1), 0.0)
                assertEquals(283.0, cursor.getDouble(2), 0.0)
                assertEquals(12.0, cursor.getDouble(3), 0.0)
                assertEquals(2.0, cursor.getDouble(4), 0.0)
                assertEquals(1.0, cursor.getDouble(5), 0.0)
                assertEquals(139500.0, cursor.getDouble(6), 0.0)
                assertEquals(136300.0, cursor.getDouble(7), 0.0)
            }

            db.query(
                "SELECT COUNT(*) FROM pay_rate_settings"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate5To6PreservesPayRateSettingsAndCreatesProfileCompensation() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
            INSERT INTO pay_rate_settings (
                id, otRate, phRate, doRate, rateSource,
                basisSalary2027, updatedAt
            ) VALUES (
                1, 283.0, 1884.0, 1884.0,
                'migration-test', 56520.0, 1710000000000
            )
            """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            AppDatabase.MIGRATION_5_6
        ).use { db ->

            db.query(
                """
            SELECT otRate, phRate, doRate, rateSource,
                   basisSalary2027, updatedAt
            FROM pay_rate_settings
            WHERE id = 1
            """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())

                assertEquals(283.0, cursor.getDouble(0), 0.0)
                assertEquals(1884.0, cursor.getDouble(1), 0.0)
                assertEquals(1884.0, cursor.getDouble(2), 0.0)
                assertEquals("migration-test", cursor.getString(3))
                assertEquals(56520.0, cursor.getDouble(4), 0.0)
                assertEquals(1710000000000, cursor.getLong(5))
            }

            db.query(
                "SELECT COUNT(*) FROM profile_compensation"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate14To15CreatesTransferActiveCacheWithoutTouchingLegacyTables() {
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL(
                """
                INSERT INTO profile (
                    id, fullName, serviceNo, unit, paySheetNo,
                    grade, basicSalary, otRate, updatedAt, salaryStep
                ) VALUES (
                    1, 'Transfer Cache Nurse', 'S-015', 'Ward 15',
                    'PS-015', 'MN 3', 120000.0, 283.0, 1710000000000, 5
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            15,
            true,
            AppDatabase.MIGRATION_14_15
        ).use { db ->
            db.query(
                """
                SELECT id,
                       requestId,
                       requestStatus,
                       currentHospitalId,
                       preferenceHospitalIdsJson,
                       matchCycleId,
                       matchType,
                       matchStatus,
                       matchPayloadJson,
                       syncStatus,
                       updatedAt
                FROM transfer_active_cache
                WHERE id = 1
                """.trimIndent()
            ).use { cursor ->
                assertEquals(0, cursor.count)
            }

            db.query(
                "SELECT fullName, serviceNo, basicSalary, otRate, salaryStep FROM profile WHERE id = 1"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("Transfer Cache Nurse", cursor.getString(0))
                assertEquals("S-015", cursor.getString(1))
                assertEquals(120000.0, cursor.getDouble(2), 0.0)
                assertEquals(283.0, cursor.getDouble(3), 0.0)
                assertEquals(5, cursor.getInt(4))
            }
        }
    }

    @Test
    fun migrate15To16PreservesTransferCacheAndAddsGradeColumn() {
        helper.createDatabase(TEST_DB, 15).apply {
            execSQL(
                """
                INSERT INTO transfer_active_cache (
                    id, requestId, requestStatus, currentHospitalId,
                    preferenceHospitalIdsJson, matchCycleId, matchType,
                    matchStatus, matchPayloadJson, syncStatus, updatedAt
                ) VALUES (
                    1, 'req-mig-15', 'PENDING', 'MOH2026-0001',
                    '["MOH2026-0002"]', NULL, NULL,
                    NULL, NULL, 'SYNCED', 1710000000000
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            16,
            true,
            AppDatabase.MIGRATION_15_16
        ).use { db ->
            db.query(
                """
                SELECT id,
                       requestId,
                       requestStatus,
                       currentHospitalId,
                       preferenceHospitalIdsJson,
                       grade,
                       syncStatus,
                       updatedAt
                FROM transfer_active_cache
                WHERE id = 1
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals("req-mig-15", cursor.getString(1))
                assertEquals("PENDING", cursor.getString(2))
                assertEquals("MOH2026-0001", cursor.getString(3))
                assertEquals("[\"MOH2026-0002\"]", cursor.getString(4))
                // Newly added column defaults to null for pre-existing rows
                assertTrue(cursor.isNull(5))
                assertEquals("SYNCED", cursor.getString(6))
                assertEquals(1710000000000L, cursor.getLong(7))
            }
        }
    }

    @Test
    fun migrate10To11PreservesSalarySteps() {
        helper.createDatabase(TEST_DB, 10).apply {
            execSQL(
                """
            INSERT INTO salary_steps_2027 (
                id, grade, salaryStep, currentBasicSalary2026,
                basicSalary2027, effectiveFrom, sourceLabel
            ) VALUES (
                51, 'MN 3', 7, 70000.0,
                77000.0, '2027-01-01', 'migration-test'
            )
            """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            11,
            true,
            AppDatabase.MIGRATION_10_11
        ).use { db ->

            db.query(
                """
            SELECT grade, salaryStep, currentBasicSalary2026,
                   basicSalary2027, effectiveFrom, sourceLabel
            FROM salary_steps_2027
            WHERE id = 51
            """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())

                assertEquals("MN 3", cursor.getString(0))
                assertEquals(7, cursor.getInt(1))
                assertEquals(70000.0, cursor.getDouble(2), 0.0)
                assertEquals(77000.0, cursor.getDouble(3), 0.0)
                assertEquals("2027-01-01", cursor.getString(4))
                assertEquals("migration-test", cursor.getString(5))
            }
        }
    }
    @Test
    fun migrate11To16PreservesExistingDataAndCreatesNewTables() {
        helper.createDatabase(TEST_DB, 11).apply {
            // 1. Seed Profile
            execSQL(
                """
                INSERT INTO profile (
                    id, fullName, serviceNo, unit, paySheetNo, grade, basicSalary, otRate, updatedAt, salaryStep
                ) VALUES (
                    101, 'Senior Sister Perera', 'SN-9988', 'ICU', 'PS-9988', 'Grade I', 135000.0, 318.5, 1715000000000, 7
                )
                """.trimIndent()
            )

            // 2. Seed Financial Record
            execSQL(
                """
                INSERT INTO financial_records (
                    id, recordMonth, timestamp, basicSalary, otRate, otHours,
                    phDays, doDays, wopDeduction, apitTaxAmount, loanDeduction,
                    otherDeductions, totalHoursWorked, grossSalary, netSalary
                ) VALUES (
                    501, '2026-09', 1715000000000, 135000.0, 318.5, 24.0,
                    1.0, 2.0, 10800.0, 3200.0, 15000.0,
                    1200.0, 184.0, 156444.0, 126244.0
                )
                """.trimIndent()
            )

            // 3. Seed Claim Period
            execSQL(
                """
                INSERT INTO claim_period (
                    id, startDate, endDate, createdAt, wardType
                ) VALUES (
                    11, '2026-09-01', '2026-09-30', 1715000000000, 'ICU'
                )
                """.trimIndent()
            )

            // 4. Seed Daily Entry
            execSQL(
                """
                INSERT INTO daily_entry (
                    id, claimPeriodId, date, isPH, isDO, isLeave, leaveType,
                    normalTimeIn, normalTimeOut, normalHours,
                    otTimeIn, otTimeOut, otHours, wardOverride, reason
                ) VALUES (
                    1101, 11, '2026-09-15', 0, 0, 0, NULL,
                    '07:00', '19:00', 12.0,
                    '19:00', '21:00', 2.0, '', ''
                )
                """.trimIndent()
            )

            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            16,
            true,
            AppDatabase.MIGRATION_11_12,
            AppDatabase.MIGRATION_12_13,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_14_15,
            AppDatabase.MIGRATION_15_16
        ).use { db ->
            // A. Verify Profile Data Preservation
            db.query(
                """
                SELECT fullName, serviceNo, unit, paySheetNo, grade, basicSalary, otRate, salaryStep
                FROM profile WHERE id = 101
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("Senior Sister Perera", cursor.getString(0))
                assertEquals("SN-9988", cursor.getString(1))
                assertEquals("ICU", cursor.getString(2))
                assertEquals("PS-9988", cursor.getString(3))
                assertEquals("Grade I", cursor.getString(4))
                assertEquals(135000.0, cursor.getDouble(5), 0.0)
                assertEquals(318.5, cursor.getDouble(6), 0.0)
                assertEquals(7, cursor.getInt(7))
            }

            // B. Verify Financial Record Data Preservation
            db.query(
                """
                SELECT recordMonth, basicSalary, otHours, grossSalary, netSalary
                FROM financial_records WHERE id = 501
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("2026-09", cursor.getString(0))
                assertEquals(135000.0, cursor.getDouble(1), 0.0)
                assertEquals(24.0, cursor.getDouble(2), 0.0)
                assertEquals(156444.0, cursor.getDouble(3), 0.0)
                assertEquals(126244.0, cursor.getDouble(4), 0.0)
            }

            // C. Verify Claim Period & Daily Entry Preservation
            db.query(
                """
                SELECT startDate, endDate, wardType FROM claim_period WHERE id = 11
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("2026-09-01", cursor.getString(0))
                assertEquals("2026-09-30", cursor.getString(1))
                assertEquals("ICU", cursor.getString(2))
            }

            db.query(
                """
                SELECT claimPeriodId, date, normalHours, otHours
                FROM daily_entry WHERE id = 1101
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(11, cursor.getInt(0))
                assertEquals("2026-09-15", cursor.getString(1))
                assertEquals(12.0, cursor.getDouble(2), 0.0)
                assertEquals(2.0, cursor.getDouble(3), 0.0)
            }

            // D. Verify Pay Sheet Documents Table, Insertion, Retrieval, and Unique Index
            db.execSQL(
                """
                INSERT INTO pay_sheet_documents (
                    monthKey, displayMonth, filePath, fileSizeBytes, sha256, createdAt, updatedAt
                ) VALUES (
                    '2026-09', 'September 2026', '/vault/2026-09.pdf', 204850, 'hash-9988', 1715000000000, 1715000005000
                )
                """.trimIndent()
            )

            db.query(
                """
                SELECT monthKey, displayMonth, filePath, fileSizeBytes, sha256, createdAt, updatedAt
                FROM pay_sheet_documents WHERE monthKey = '2026-09'
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("2026-09", cursor.getString(0))
                assertEquals("September 2026", cursor.getString(1))
                assertEquals("/vault/2026-09.pdf", cursor.getString(2))
                assertEquals(204850L, cursor.getLong(3))
                assertEquals("hash-9988", cursor.getString(4))
                assertEquals(1715000000000L, cursor.getLong(5))
                assertEquals(1715000005000L, cursor.getLong(6))
            }

            try {
                db.execSQL(
                    """
                    INSERT INTO pay_sheet_documents (
                        monthKey, displayMonth, filePath, fileSizeBytes, sha256, createdAt, updatedAt
                    ) VALUES (
                        '2026-09', 'Duplicate September 2026', '/vault/duplicate.pdf', 1024, 'hash-dup', 1715000010000, 1715000010000
                    )
                    """.trimIndent()
                )
                fail("Expected SQLiteConstraintException when inserting duplicate monthKey into pay_sheet_documents")
            } catch (expected: SQLiteConstraintException) {
                // Expected: index_pay_sheet_documents_monthKey enforces uniqueness
            }

            // E. Verify profile_additional_allowances exists and is writable
            db.execSQL(
                """
                INSERT INTO profile_additional_allowances (name, amount, updatedAt)
                VALUES ('Special Risk Allowance', 1500.0, 1715000000000)
                """.trimIndent()
            )
            db.query("SELECT name, amount FROM profile_additional_allowances WHERE name = 'Special Risk Allowance'").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("Special Risk Allowance", cursor.getString(0))
                assertEquals(1500.0, cursor.getDouble(1), 0.0)
            }

            // F. Verify profile_deductions exists and is writable
            db.execSQL(
                """
                INSERT INTO profile_deductions (claimPeriodId, name, amount, updatedAt)
                VALUES (11, 'Welfare Society', 500.0, 1715000000000)
                """.trimIndent()
            )
            db.query("SELECT claimPeriodId, name, amount FROM profile_deductions WHERE name = 'Welfare Society'").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(11, cursor.getInt(0))
                assertEquals("Welfare Society", cursor.getString(1))
                assertEquals(500.0, cursor.getDouble(2), 0.0)
            }

            // G. Verify transfer_active_cache exists with nullable grade column
            db.execSQL(
                """
                INSERT INTO transfer_active_cache (
                    id, requestId, requestStatus, currentHospitalId,
                    preferenceHospitalIdsJson, grade, matchCycleId, matchType,
                    matchStatus, matchPayloadJson, syncStatus, updatedAt
                ) VALUES (
                    1, 'req-e2e-101', 'SUBMITTED', 'HOSP-001',
                    '["HOSP-002"]', NULL, 'CYCLE-1', 'DIRECT',
                    'PENDING', NULL, 'SYNCED', 1715000000000
                )
                """.trimIndent()
            )
            db.query("SELECT requestId, grade, syncStatus FROM transfer_active_cache WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("req-e2e-101", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertEquals("SYNCED", cursor.getString(2))
            }
        }
    }

    @Test
    fun migrate16To17PreservesExistingDataAndCreatesKnowledgeHubTables() {
        helper.createDatabase(TEST_DB, 16).apply {
            // Seed representative existing data across existing tables
            execSQL(
                """
                INSERT INTO profile (
                    id, fullName, serviceNo, unit, paySheetNo, grade, basicSalary, otRate, updatedAt, salaryStep
                ) VALUES (
                    101, 'Senior Sister Perera', 'SN-9988', 'ICU', 'PS-9988', 'Grade I', 135000.0, 318.5, 1715000000000, 7
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO cpd_logs (
                    id, seminarTitle, date, earnedPoints, speakerOrInstitution, notes
                ) VALUES (
                    1, 'Advanced Sepsis Management', 1715000000000, 5, 'College of Anesthesiologists', 'Focus on fluid resuscitation protocols'
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO financial_records (
                    id, recordMonth, timestamp, basicSalary, otRate, otHours,
                    phDays, doDays, wopDeduction, apitTaxAmount, loanDeduction,
                    otherDeductions, totalHoursWorked, grossSalary, netSalary
                ) VALUES (
                    501, '2026-09', 1715000000000, 135000.0, 318.5, 24.0,
                    1.0, 2.0, 10800.0, 3200.0, 15000.0,
                    1200.0, 184.0, 156444.0, 126244.0
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO claim_period (
                    id, startDate, endDate, createdAt, wardType
                ) VALUES (
                    11, '2026-09-01', '2026-09-30', 1715000000000, 'ICU'
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO daily_entry (
                    id, claimPeriodId, date, isPH, isDO, isLeave, leaveType,
                    normalTimeIn, normalTimeOut, normalHours,
                    otTimeIn, otTimeOut, otHours, wardOverride, reason
                ) VALUES (
                    1101, 11, '2026-09-15', 0, 0, 0, NULL,
                    '07:00', '19:00', 12.0,
                    '19:00', '21:00', 2.0, '', ''
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO isbar_notes (
                    id, patientIdentifier, identification, situation, background,
                    assessment, recommendation, timestamp
                ) VALUES (
                    1, 'BED-04', 'Staff Nurse A', 'Deteriorating SpO2', 'COPD exacerbation',
                    'SpO2 88% on room air', 'Start 4L O2 via nasal prongs', 1715000000000
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO clinical_tasks (
                    id, taskName, description, priority, triggerTime, isCompleted, bypassDnd
                ) VALUES (
                    1, 'ABG Sampling', 'Draw arterial blood gas for Bed 4', 'HIGH', 1715000000000, 0, 1
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO pay_rate_settings (
                    id, otRate, phRate, doRate, rateSource, basisSalary2027, updatedAt
                ) VALUES (
                    1, 318.5, 637.0, 477.75, 'CIRCULAR_2026', 145000.0, 1715000000000
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO profile_compensation (
                    id, riskAllowance, claAllowance, additionalAllowancesTotal, totalDeductions, updatedAt
                ) VALUES (
                    1, 3000.0, 5000.0, 8000.0, 25000.0, 1715000000000
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO profile_additional_allowances (
                    id, name, amount, updatedAt
                ) VALUES (
                    1, 'Uniform Allowance', 1200.0, 1715000000000
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO profile_deductions (
                    id, claimPeriodId, name, amount, updatedAt
                ) VALUES (
                    1, 11, 'Welfare Society', 500.0, 1715000000000
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO salary_steps_2027 (
                    id, grade, salaryStep, currentBasicSalary2026, basicSalary2027, effectiveFrom, sourceLabel
                ) VALUES (
                    1, 'Grade I', 7, 135000.0, 145000.0, '2027-01-01', 'MoH 2026 Revision'
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO pay_sheet_documents (
                    id, monthKey, displayMonth, filePath, fileSizeBytes, sha256, createdAt, updatedAt
                ) VALUES (
                    1, '2026-09', 'September 2026', '/vault/2026-09.pdf', 204850, 'hash-9988', 1715000000000, 1715000005000
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO transfer_active_cache (
                    id, requestId, requestStatus, currentHospitalId,
                    preferenceHospitalIdsJson, grade, matchCycleId, matchType,
                    matchStatus, matchPayloadJson, syncStatus, updatedAt
                ) VALUES (
                    1, 'req-e2e-101', 'SUBMITTED', 'HOSP-001',
                    '["HOSP-002"]', 'Grade I', 'CYCLE-1', 'DIRECT',
                    'PENDING', NULL, 'SYNCED', 1715000000000
                )
                """.trimIndent()
            )

            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            17,
            true,
            AppDatabase.MIGRATION_16_17
        ).use { db ->
            // Enable SQLite foreign key enforcement
            db.execSQL("PRAGMA foreign_keys = ON")

            // 1. Verify all 14 existing tables and their data remain intact
            val existingTables = listOf(
                "profile",
                "claim_period",
                "daily_entry",
                "financial_records",
                "isbar_notes",
                "clinical_tasks",
                "cpd_logs",
                "pay_rate_settings",
                "profile_compensation",
                "profile_additional_allowances",
                "profile_deductions",
                "salary_steps_2027",
                "pay_sheet_documents",
                "transfer_active_cache"
            )
            for (tableName in existingTables) {
                db.query("SELECT count(*) FROM `$tableName`").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Table $tableName must have 1 pre-existing row", 1, cursor.getInt(0))
                }
            }

            // 2. Verify cpd_logs columns and data remain unchanged
            db.query(
                "SELECT id, seminarTitle, date, earnedPoints, speakerOrInstitution, notes FROM cpd_logs WHERE id = 1"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals("Advanced Sepsis Management", cursor.getString(1))
                assertEquals(1715000000000L, cursor.getLong(2))
                assertEquals(5, cursor.getInt(3))
                assertEquals("College of Anesthesiologists", cursor.getString(4))
                assertEquals("Focus on fluid resuscitation protocols", cursor.getString(5))
            }

            // 3. Verify all nine new Knowledge Hub tables exist
            val newTables = listOf(
                "knowledge_sources",
                "knowledge_categories",
                "knowledge_articles",
                "knowledge_tags",
                "article_tag_cross_ref",
                "knowledge_bookmarks",
                "knowledge_personal_notes",
                "knowledge_reading_history",
                "knowledge_offline_cache"
            )
            for (tableName in newTables) {
                db.query("SELECT count(*) FROM `$tableName`").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("New table $tableName must start empty", 0, cursor.getInt(0))
                }
            }

            // 4. Verify knowledge_sources and knowledge_articles insertion & RESTRICT foreign key
            db.execSQL(
                """
                INSERT INTO knowledge_sources (
                    id, name, shortCode, trustTier, websiteUrl, isSystemDefault, updatedAt
                ) VALUES (
                    'SRC-MOH', 'Ministry of Health Sri Lanka', 'MOH-SL', 'GOVERNMENT_OFFICIAL',
                    'https://health.gov.lk', 1, 1715000000000
                )
                """.trimIndent()
            )

            // Attempting to insert article with invalid sourceInstitution must fail RESTRICT FK check
            try {
                db.execSQL(
                    """
                    INSERT INTO knowledge_articles (
                        id, title, sourceInstitution, category, clinicalSummary,
                        recommendationsJson, evidenceGrade, year, lastReviewed,
                        isFullTextAvailable, isSuperseded, createdAt, updatedAt
                    ) VALUES (
                        'ART-INVALID', 'Invalid Article', 'NON_EXISTENT_SRC', 'CLINICAL', 'Summary',
                        '["Rec 1"]', 'GRADE_A', 2026, '2026-05', 0, 0, 1715000000000, 1715000000000
                    )
                    """.trimIndent()
                )
                fail("Expected SQLiteConstraintException when foreign key sourceInstitution does not exist")
            } catch (expected: SQLiteConstraintException) {
                // Expected RESTRICT foreign key violation
            }

            // Insert valid article
            db.execSQL(
                """
                INSERT INTO knowledge_articles (
                    id, title, sourceInstitution, category, clinicalSummary,
                    recommendationsJson, evidenceGrade, year, lastReviewed,
                    isFullTextAvailable, isSuperseded, createdAt, updatedAt
                ) VALUES (
                    'ART-SEPSIS-2026', 'National Sepsis Protocol 2026', 'SRC-MOH', 'CRITICAL_CARE',
                    'Standard evidence-based protocol for early sepsis resuscitation in wards and ICUs.',
                    '["Measure lactate", "Administer broad-spectrum antibiotics within 1 hour", "30ml/kg crystalloid bolus"]',
                    'GRADE_A', 2026, '2026-03-15', 1, 0, 1715000000000, 1715000000000
                )
                """.trimIndent()
            )

            // Attempting to delete source when referenced by article must fail (RESTRICT)
            try {
                db.execSQL("DELETE FROM knowledge_sources WHERE id = 'SRC-MOH'")
                fail("Expected SQLiteConstraintException when deleting source referenced by article")
            } catch (expected: SQLiteConstraintException) {
                // Expected RESTRICT foreign key violation
            }

            // 5. Verify tags and article_tag_cross_ref
            // Verify inserting cross ref with nonexistent tag fails FK check
            try {
                db.execSQL("INSERT INTO article_tag_cross_ref (articleId, tag) VALUES ('ART-SEPSIS-2026', 'NON_EXISTENT_TAG')")
                fail("Expected SQLiteConstraintException when tag foreign key does not exist")
            } catch (expected: SQLiteConstraintException) {
                // Expected FK constraint failure
            }

            db.execSQL("INSERT INTO knowledge_tags (tag) VALUES ('sepsis')")
            db.execSQL("INSERT INTO article_tag_cross_ref (articleId, tag) VALUES ('ART-SEPSIS-2026', 'sepsis')")
            db.query("SELECT tag FROM article_tag_cross_ref WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("sepsis", cursor.getString(0))
            }

            // Verify knowledge_categories table
            db.execSQL("INSERT INTO knowledge_categories (id, displayName, displayOrder) VALUES ('CRITICAL_CARE', 'Critical Care', 1)")
            db.query("SELECT displayName, displayOrder FROM knowledge_categories WHERE id = 'CRITICAL_CARE'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Critical Care", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }

            // 6. Verify Bookmarks and CASCADE behavior
            // Inserting bookmark referencing nonexistent article must fail
            try {
                db.execSQL("INSERT INTO knowledge_bookmarks (articleId, bookmarkedAt) VALUES ('NON-EXISTENT', 1715000000000)")
                fail("Expected SQLiteConstraintException for bookmark referencing nonexistent article")
            } catch (expected: SQLiteConstraintException) {
                // Expected FK constraint failure
            }

            db.execSQL("INSERT INTO knowledge_bookmarks (articleId, bookmarkedAt) VALUES ('ART-SEPSIS-2026', 1715000000000)")
            db.query("SELECT articleId FROM knowledge_bookmarks WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertEquals(1, cursor.count)
            }

            // 7. Verify Offline Cache and Unique Index on localRelativePath
            db.execSQL(
                """
                INSERT INTO knowledge_offline_cache (
                    articleId, localRelativePath, mimeType, fileSizeBytes, sha256Hash, cachedAt, lastAccessedAt, isFullTextCached
                ) VALUES (
                    'ART-SEPSIS-2026', 'guidelines/sepsis_2026.pdf', 'application/pdf', 1048576, 'sha256-sepsis', 1715000000000, 1715000000000, 1
                )
                """.trimIndent()
            )

            // Insert second article for testing unique index on localRelativePath
            db.execSQL(
                """
                INSERT INTO knowledge_articles (
                    id, title, sourceInstitution, category, clinicalSummary,
                    recommendationsJson, evidenceGrade, year, lastReviewed,
                    isFullTextAvailable, isSuperseded, createdAt, updatedAt
                ) VALUES (
                    'ART-DENGUE-2026', 'National Dengue Protocol', 'SRC-MOH', 'INFECTIOUS_DISEASES',
                    'Dengue clinical management guidelines.', '["Fluid balance chart"]',
                    'GRADE_A', 2026, '2026-04-01', 1, 0, 1715000000000, 1715000000000
                )
                """.trimIndent()
            )

            try {
                db.execSQL(
                    """
                    INSERT INTO knowledge_offline_cache (
                        articleId, localRelativePath, mimeType, fileSizeBytes, sha256Hash, cachedAt, lastAccessedAt, isFullTextCached
                    ) VALUES (
                        'ART-DENGUE-2026', 'guidelines/sepsis_2026.pdf', 'application/pdf', 524288, 'sha256-dengue', 1715000000000, 1715000000000, 1
                    )
                    """.trimIndent()
                )
                fail("Expected SQLiteConstraintException for duplicate localRelativePath")
            } catch (expected: SQLiteConstraintException) {
                // Expected unique index constraint violation
            }

            // 8. Verify Personal Notes with ON DELETE SET NULL and preservation of originalArticleId
            db.execSQL(
                """
                INSERT INTO knowledge_personal_notes (
                    articleId, originalArticleId, noteContent, createdAt, updatedAt
                ) VALUES (
                    'ART-SEPSIS-2026', 'ART-SEPSIS-2026', 'Crucial ICU bedside protocol. Review fluid bolus rule with junior staff.', 1715000000000, 1715000000000
                )
                """.trimIndent()
            )

            // 9. Verify Reading History with SET_NULL on CPD log deletion
            db.execSQL(
                """
                INSERT INTO knowledge_reading_history (
                    articleId, lastReadAt, readingProgressPercent, isCompleted, completedAt, linkedCpdLogId
                ) VALUES (
                    'ART-SEPSIS-2026', 1715000000000, 100, 1, 1715000005000, 1
                )
                """.trimIndent()
            )

            // Verify linkedCpdLogId is currently 1
            db.query("SELECT linkedCpdLogId FROM knowledge_reading_history WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }

            // Delete CPD log #1 -> must set linkedCpdLogId to NULL in reading history (ON DELETE SET NULL)
            db.execSQL("DELETE FROM cpd_logs WHERE id = 1")
            db.query("SELECT linkedCpdLogId FROM knowledge_reading_history WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue("linkedCpdLogId must be null after deleting referenced CPD log", cursor.isNull(0))
            }

            // Delete article 'ART-SEPSIS-2026' ->
            // - bookmark must be CASCADE deleted
            // - offline cache must be CASCADE deleted
            // - reading history must be CASCADE deleted
            // - note must survive with articleId set to NULL, and originalArticleId preserved!
            db.execSQL("DELETE FROM knowledge_articles WHERE id = 'ART-SEPSIS-2026'")

            // Bookmark cascaded
            db.query("SELECT count(*) FROM knowledge_bookmarks WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            // Cache cascaded
            db.query("SELECT count(*) FROM knowledge_offline_cache WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            // Reading history cascaded
            db.query("SELECT count(*) FROM knowledge_reading_history WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            // Tag junction cascaded
            db.query("SELECT count(*) FROM article_tag_cross_ref WHERE articleId = 'ART-SEPSIS-2026'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            // Note survived with articleId = NULL and originalArticleId intact
            db.query(
                "SELECT articleId, originalArticleId, noteContent FROM knowledge_personal_notes WHERE originalArticleId = 'ART-SEPSIS-2026'"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertTrue("articleId foreign key must be set null upon article deletion", cursor.isNull(0))
                assertEquals("ART-SEPSIS-2026", cursor.getString(1))
                assertEquals("Crucial ICU bedside protocol. Review fluid bolus rule with junior staff.", cursor.getString(2))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
