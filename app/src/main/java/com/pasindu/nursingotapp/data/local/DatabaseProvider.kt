package com.pasindu.nursingotapp.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "nursing_ot_app.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration() // Automatically clears and recreates tables on hash mismatches during development
            .build()
    }
}