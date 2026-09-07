package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.KnowledgeHubDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.domain.usecase.AddClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.AddCpdLogUseCase
import com.pasindu.nursingotapp.domain.usecase.AddIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.ApplyMatched2027DayRateUseCase
import com.pasindu.nursingotapp.domain.usecase.CalculateDailyEntryHoursUseCase
import com.pasindu.nursingotapp.domain.usecase.CalculateFinanceSummaryUseCase
import com.pasindu.nursingotapp.domain.usecase.CreateClaimPeriodUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteAllClaimPeriodsUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteClaimPeriodUseCase
import com.pasindu.nursingotapp.domain.usecase.EnsureManualPayRateRecordUseCase
import com.pasindu.nursingotapp.domain.usecase.GetDailyEntryForDateUseCase
import com.pasindu.nursingotapp.domain.usecase.MatchSalaryStepUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClinicalTasksUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveCpdLogsUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileUseCase
import com.pasindu.nursingotapp.domain.usecase.PurgeOldIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveDailyEntryUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceRatesUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveProfileUseCase
import com.pasindu.nursingotapp.domain.usecase.SetClinicalTaskCompletedUseCase
import com.pasindu.nursingotapp.domain.usecase.SynchronizePolicyRatesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides fun provideObserveProfileUseCase(dao: ProfileDao) = ObserveProfileUseCase(dao)
    @Provides fun provideObserveProfileCompensationUseCase(dao: ProfileCompensationDao) = ObserveProfileCompensationUseCase(dao)
    @Provides fun provideObserveOtRateUseCase(dao: PayRateSettingsDao) = ObserveOtRateUseCase(dao)
    @Provides fun provideSaveProfileUseCase(dao: ProfileDao) = SaveProfileUseCase(dao)
    @Provides fun provideSaveProfileCompensationUseCase(dao: ProfileCompensationDao) = SaveProfileCompensationUseCase(dao)
    @Provides fun provideSaveOtRateUseCase(dao: PayRateSettingsDao) = SaveOtRateUseCase(dao)
    @Provides fun provideMatchSalaryStepUseCase(dao: SalaryStep2027Dao) = MatchSalaryStepUseCase(dao)
    @Provides fun provideApplyMatched2027DayRateUseCase(dao: PayRateSettingsDao) = ApplyMatched2027DayRateUseCase(dao)
    @Provides fun provideCalculateDailyEntryHoursUseCase() = CalculateDailyEntryHoursUseCase()
    @Provides fun provideCalculateFinanceSummaryUseCase() = CalculateFinanceSummaryUseCase()
    @Provides fun provideEnsureManualPayRateRecordUseCase(dao: PayRateSettingsDao) = EnsureManualPayRateRecordUseCase(dao)
    @Provides fun provideSynchronizePolicyRatesUseCase(payRateSettingsDao: PayRateSettingsDao, salaryStep2027Dao: SalaryStep2027Dao) = SynchronizePolicyRatesUseCase(payRateSettingsDao, salaryStep2027Dao)
    @Provides fun provideSaveFinanceCompensationUseCase(dao: ProfileCompensationDao) = SaveFinanceCompensationUseCase(dao)
    @Provides fun provideSaveFinanceRatesUseCase(dao: PayRateSettingsDao) = SaveFinanceRatesUseCase(dao)
    @Provides fun provideObserveClaimPeriodsUseCase(dao: ClaimPeriodDao) = ObserveClaimPeriodsUseCase(dao)
    @Provides fun provideCreateClaimPeriodUseCase(dao: ClaimPeriodDao) = CreateClaimPeriodUseCase(dao)
    @Provides fun provideDeleteClaimPeriodUseCase(claimPeriodDao: ClaimPeriodDao, dailyEntryDao: DailyEntryDao) = DeleteClaimPeriodUseCase(claimPeriodDao, dailyEntryDao)
    @Provides fun provideDeleteAllClaimPeriodsUseCase(claimPeriodDao: ClaimPeriodDao, dailyEntryDao: DailyEntryDao) = DeleteAllClaimPeriodsUseCase(claimPeriodDao, dailyEntryDao)
    @Provides fun provideObserveClaimDailyEntriesUseCase(dao: DailyEntryDao) = ObserveClaimDailyEntriesUseCase(dao)
    @Provides fun provideSaveDailyEntryUseCase(dao: DailyEntryDao) = SaveDailyEntryUseCase(dao)
    @Provides fun provideGetDailyEntryForDateUseCase(dao: DailyEntryDao) = GetDailyEntryForDateUseCase(dao)
    @Provides fun provideObserveCpdLogsUseCase(dao: KnowledgeHubDao) = ObserveCpdLogsUseCase(dao)
    @Provides fun provideAddCpdLogUseCase(dao: KnowledgeHubDao) = AddCpdLogUseCase(dao)
    @Provides fun provideObserveIsbarNotesUseCase(dao: com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao) = ObserveIsbarNotesUseCase(dao)
    @Provides fun provideObserveClinicalTasksUseCase(dao: com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao) = ObserveClinicalTasksUseCase(dao)
    @Provides fun provideAddIsbarNoteUseCase(dao: com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao) = AddIsbarNoteUseCase(dao)
    @Provides fun providePurgeOldIsbarNotesUseCase(dao: com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao) = PurgeOldIsbarNotesUseCase(dao)
    @Provides fun provideAddClinicalTaskUseCase(dao: com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao) = AddClinicalTaskUseCase(dao)
    @Provides fun provideSetClinicalTaskCompletedUseCase(dao: com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao) = SetClinicalTaskCompletedUseCase(dao)
}
