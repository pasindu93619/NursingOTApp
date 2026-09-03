package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.domain.usecase.ApplyMatched2027DayRateUseCase
import com.pasindu.nursingotapp.domain.usecase.MatchSalaryStepUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveProfileUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideObserveProfileUseCase(dao: ProfileDao) = ObserveProfileUseCase(dao)

    @Provides
    fun provideObserveProfileCompensationUseCase(dao: ProfileCompensationDao) =
        ObserveProfileCompensationUseCase(dao)

    @Provides
    fun provideObserveOtRateUseCase(dao: PayRateSettingsDao) = ObserveOtRateUseCase(dao)

    @Provides
    fun provideSaveProfileUseCase(dao: ProfileDao) = SaveProfileUseCase(dao)

    @Provides
    fun provideSaveProfileCompensationUseCase(dao: ProfileCompensationDao) =
        SaveProfileCompensationUseCase(dao)

    @Provides
    fun provideSaveOtRateUseCase(dao: PayRateSettingsDao) = SaveOtRateUseCase(dao)

    @Provides
    fun provideMatchSalaryStepUseCase(dao: SalaryStep2027Dao) = MatchSalaryStepUseCase(dao)

    @Provides
    fun provideApplyMatched2027DayRateUseCase(dao: PayRateSettingsDao) =
        ApplyMatched2027DayRateUseCase(dao)
}
