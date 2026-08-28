package com.pasindu.nursingotapp.data.local

import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Seeds the exact supplied salary table once when the lookup table is empty. */
object SalaryTableSeeder {
    fun seedIfEmpty(dao: SalaryStep2027Dao) {
        CoroutineScope(Dispatchers.IO).launch {
            if (dao.count() == 0) {
                dao.insertAll(SalaryTable2026_2027Seed.rows)
            }
        }
    }
}
