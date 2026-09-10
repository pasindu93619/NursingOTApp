package com.pasindu.nursingotapp.data.local

import android.content.Context
import androidx.room.Room

/**
 * Centralized legacy database builder used during the incremental Hilt migration.
 *
 * This helper intentionally delegates all migration registration to
 * [DatabaseMigrationRegistry] so no second migration list is maintained.
 */
@Deprecated(
    message = "Prefer injecting AppDatabase with Hilt. This helper exists for migration compatibility only.",
    level = DeprecationLevel.WARNING
)
object DatabaseProviderCentralized {

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "nursing_ot_app.db"
            )
                .addMigrations(*DatabaseMigrationRegistry.ALL_MIGRATIONS)
                .build()
                .also {
                    SalaryTableSeeder.seedIfNeeded(it.salaryStep2027Dao())
                    instance = it
                }
        }
    }
}
