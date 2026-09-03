package com.pasindu.nursingotapp.data.local

import androidx.room.migration.Migration

/**
 * Single source of truth for Room migrations.
 *
 * Every schema change must register its migration here. Database builders
 * must consume [ALL_MIGRATIONS] rather than maintaining separate lists.
 */
object DatabaseMigrationRegistry {

    /**
     * Complete ordered migration set supported by the current database.
     *
     * Keep every migration that may be required to upgrade an existing user
     * database. Do not remove older migrations just because the latest version
     * has advanced.
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
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

    const val CURRENT_DATABASE_VERSION = 12

    /**
     * Naming convention for future migrations:
     * MIGRATION_<oldVersion>_<newVersion>
     */
    const val MIGRATION_NAMING_CONVENTION = "MIGRATION_<oldVersion>_<newVersion>"
}
