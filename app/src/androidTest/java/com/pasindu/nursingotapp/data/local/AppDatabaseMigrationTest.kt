package com.pasindu.nursingotapp.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private lateinit var helper: MigrationTestHelper
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
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
                "SELECT monthYear, basicSalary, calculatedOtAmount, netSalary FROM financial_records WHERE id = 17"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertNotNull(cursor)
                cursor.moveToFirst()
                assertEquals("2026-08", cursor.getString(0))
                assertEquals(120000.0, cursor.getDouble(1), 0.0)
                assertEquals(4500.0, cursor.getDouble(2), 0.0)
                assertEquals(136800.0, cursor.getDouble(3), 0.0)
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
    fun migrate9To10MustPreserveSalarySteps() {
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
    fun migrate11To12CreatesPaySheetDocumentsWithoutTouchingExistingData() {
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
            db.execSQL(
                """
                INSERT INTO pay_sheet_documents (
                    monthKey, displayMonth, filePath, fileSizeBytes, sha256, createdAt, updatedAt
                ) VALUES ('2026-08', 'August 2026', '/data/pay-sheet.pdf', 1024, 'abc123', 1710000000000, 1710000005000)
                """.trimIndent()
            )

            db.query("SELECT fullName, salaryStep FROM profile WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("Migration Nurse", cursor.getString(0))
                assertEquals(5, cursor.getInt(1))
            }

            db.query("SELECT monthKey, sha256 FROM pay_sheet_documents WHERE monthKey = '2026-08'").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("2026-08", cursor.getString(0))
                assertEquals("abc123", cursor.getString(1))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
