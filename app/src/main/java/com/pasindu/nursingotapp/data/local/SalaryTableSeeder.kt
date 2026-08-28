package com.pasindu.nursingotapp.data.local

import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ensures the exact supplied salary lookup table is available.
 * Only the salary lookup table is touched; profile and legacy claim data are untouched.
 */
object SalaryTableSeeder {
    fun seedIfNeeded(dao: SalaryStep2027Dao) {
        CoroutineScope(Dispatchers.IO).launch {
            val count = dao.count()
            if (count != SalaryTable2026_2027Seed.rows.size) {
                dao.clearAll()
                dao.insertAll(SalaryTable2026_2027Seed.rows)
            }
        }
    }
}
