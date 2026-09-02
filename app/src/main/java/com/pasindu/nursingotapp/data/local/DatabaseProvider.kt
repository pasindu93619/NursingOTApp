package com.pasindu.nursingotapp.data.local

import android.content.Context

/**
 * Legacy compatibility shim.
 *
 * Room database construction is owned by Hilt's DatabaseModule. New code must
 * inject AppDatabase/DAOs rather than obtaining the database statically.
 */
@Deprecated(
    message = "Inject AppDatabase or the required DAO with Hilt instead.",
    level = DeprecationLevel.WARNING
)
object DatabaseProvider {

    /**
     * Kept temporarily for legacy UI paths that have not yet moved to Hilt.
     * This delegates to the single Hilt-managed database instance when the
     * application is configured with NursingOTAppApplication.
     */
    @Deprecated(
        message = "Use Hilt injection instead of DatabaseProvider.getDatabase().",
        level = DeprecationLevel.WARNING
    )
    fun getDatabase(context: Context): AppDatabase =
        (context.applicationContext as com.pasindu.nursingotapp.NursingOTAppApplication)
            .appDatabase
}
