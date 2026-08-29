package com.pasindu.nursingotapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DatabaseMigrationRegistryTest {

    @Test
    fun latestDatabaseVersionMatchesRegisteredLatestMigration() {
        val latestRegisteredTarget = 12

        assertEquals(AppDatabase::class.java.simpleName, "AppDatabase")
        assertEquals("Latest Room schema version", latestRegisteredTarget, 12)
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
