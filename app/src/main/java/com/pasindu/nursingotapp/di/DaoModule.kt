package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.data.local.AppDatabase
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.FinancialDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeHubDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.dao.PaySheetDocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideClaimPeriodDao(database: AppDatabase): ClaimPeriodDao = database.claimPeriodDao()

    @Provides
    fun provideDailyEntryDao(database: AppDatabase): DailyEntryDao = database.dailyEntryDao()

    @Provides
    fun provideFinancialDao(database: AppDatabase): FinancialDao = database.financialDao()

    @Provides
    fun provideClinicalPlanningDao(database: AppDatabase) = database.clinicalPlanningDao()

    @Provides
    fun provideKnowledgeHubDao(database: AppDatabase): KnowledgeHubDao = database.knowledgeHubDao()

    @Provides
    fun providePayRateSettingsDao(database: AppDatabase): PayRateSettingsDao = database.payRateSettingsDao()

    @Provides
    fun provideProfileCompensationDao(database: AppDatabase): ProfileCompensationDao = database.profileCompensationDao()

    @Provides
    fun provideSalaryStep2027Dao(database: AppDatabase): SalaryStep2027Dao = database.salaryStep2027Dao()

    @Provides
    fun providePaySheetDocumentDao(database: AppDatabase): PaySheetDocumentDao = database.paySheetDocumentDao()
}
