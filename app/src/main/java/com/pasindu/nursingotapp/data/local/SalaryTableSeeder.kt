package com.pasindu.nursingotapp.data.local

import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ensures the exact supplied salary lookup table is available.
 * Repairs incomplete or stale lookup data without touching profile or claim data.
 */
object SalaryTableSeeder {
    fun seedIfNeeded(dao: SalaryStep2027Dao) {
        CoroutineScope(Dispatchers.IO).launch {
            val expected = SalaryTable2026_2027Seed.rows
            val actual = dao.observeAll().first()

            val needsRepair =
                actual.size != expected.size ||
                    expected.any { expectedRow ->
                        actual.none { stored ->
                            stored.grade == expectedRow.grade &&
                                stored.salaryStep == expectedRow.salaryStep &&
                                stored.currentBasicSalary2026 == expectedRow.currentBasicSalary2026 &&
                                stored.basicSalary2027 == expectedRow.basicSalary2027
                        }
                    }

            if (needsRepair) {
                dao.clearAll()
                dao.insertAll(expected)
            }
        }
    }
}
