package com.pasindu.nursingotapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationRegistryTest {

    @Test
    fun latestDatabaseVersionMatchesRegistry() {
        assertEquals(
            "Latest Room schema version",
            DatabaseMigrationRegistry.CURRENT_DATABASE_VERSION,
            12
        )
    }

    @Test
    fun registryContainsAllCurrentMigrations() {
        val migrationKeys = DatabaseMigrationRegistry.ALL_MIGRATIONS
            .map { "${it.startVersion}->${it.endVersion}" }

        assertEquals(
            listOf(
                "1->2",
                "2->3",
                "1->3",
                "3->4",
                "4->5",
                "5->6",
                "6->7",
                "7->8",
                "8->9",
                "9->10",
                "10->11",
                "11->12"
            ),
            migrationKeys
        )
    }

    @Test
    fun registryContainsMigrationIntoLatestVersion() {
        assertTrue(
            "Migration registry must contain a migration into the latest version",
            DatabaseMigrationRegistry.ALL_MIGRATIONS.any {
                it.endVersion == DatabaseMigrationRegistry.CURRENT_DATABASE_VERSION
            }
        )
    }

    @Test
    fun migrationNamingConventionIsStable() {
        assertEquals(
            "MIGRATION_<oldVersion>_<newVersion>",
            DatabaseMigrationRegistry.MIGRATION_NAMING_CONVENTION
        )
    }

    @Test
    fun destructiveMigrationFallbackMustRemainDisabled() {
        val providerSource = DatabaseProvider::class.java
            .declaredMethods
            .joinToString(separator = "") { it.toString() }

        assertFalse(
            "DatabaseProvider must not rely on destructive migration fallback",
            providerSource.contains("fallbackToDestructiveMigration", ignoreCase = true)
        )
    }
}
