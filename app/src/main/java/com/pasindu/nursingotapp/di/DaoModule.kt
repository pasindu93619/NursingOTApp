package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    fun provideProfileDao(database: AppDatabase) = database.profileDao()

    @Provides
    fun provideClaimPeriodDao(database: AppDatabase) = database.claimPeriodDao()

    @Provides
    fun provideDailyEntryDao(database: AppDatabase) = database.dailyEntryDao()

    @Provides
    fun provideFinancialDao(database: AppDatabase) = database.financialDao()

    @Provides
    fun provideClinicalPlanningDao(database: AppDatabase) = database.clinicalPlanningDao()

    @Provides
    fun provideKnowledgeHubDao(database: AppDatabase) = database.knowledgeHubDao()

    @Provides
    fun providePayRateSettingsDao(database: AppDatabase) = database.payRateSettingsDao()

    @Provides
    fun provideProfileCompensationDao(database: AppDatabase) = database.profileCompensationDao()

    @Provides
    fun provideSalaryStep2027Dao(database: AppDatabase) = database.salaryStep2027Dao()

    @Provides
    fun providePaySheetDocumentDao(database: AppDatabase) = database.paySheetDocumentDao()
}
