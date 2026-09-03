package com.pasindu.nursingotapp.data.local

import android.content.Context
import androidx.room.Room

/**
 * Legacy compatibility shim for screens that have not yet been migrated to Hilt.
 *
 * New code must inject AppDatabase/DAOs with Hilt. This provider remains only
 * to preserve existing functionality during the incremental architecture cleanup.
 */
@Deprecated(
    message = "Inject AppDatabase or the required DAO with Hilt instead.",
    level = DeprecationLevel.WARNING
)
object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    @Deprecated(
        message = "Use Hilt injection instead of DatabaseProvider.getDatabase().",
        level = DeprecationLevel.WARNING
    )
    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        val database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "nursing_ot_app.db"
        )
            .addMigrations(*DatabaseMigrationRegistry.ALL_MIGRATIONS)
            .build()

        SalaryTableSeeder.seedIfNeeded(database.salaryStep2027Dao())
        return database
    }
}
