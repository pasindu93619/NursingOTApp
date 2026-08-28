package com.pasindu.nursingotapp.data.local

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_1_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .fallbackToDestructiveMigration()
            .build()
    }
}