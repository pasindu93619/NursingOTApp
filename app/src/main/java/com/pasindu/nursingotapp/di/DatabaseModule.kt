package com.pasindu.nursingotapp.di

import android.content.Context
import androidx.room.Room
import com.pasindu.nursingotapp.data.local.AppDatabase
import com.pasindu.nursingotapp.data.local.DatabaseMigrationRegistry
import com.pasindu.nursingotapp.data.local.SalaryTableSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nursing_ot_app.db"
        )
            .addMigrations(*DatabaseMigrationRegistry.ALL_MIGRATIONS)
            .build()

        SalaryTableSeeder.seedIfNeeded(database.salaryStep2027Dao())
        return database
    }
}
