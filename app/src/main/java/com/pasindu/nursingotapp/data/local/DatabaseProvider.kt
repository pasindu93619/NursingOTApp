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
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_1_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12
            )
            .build()

        SalaryTableSeeder.seedIfNeeded(database.salaryStep2027Dao())
        return database
    }
}
