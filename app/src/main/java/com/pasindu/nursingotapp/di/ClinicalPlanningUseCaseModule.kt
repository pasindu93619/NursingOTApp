package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.domain.usecase.AddClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.AddIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClinicalTasksUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveShiftWorkloadUseCase
import com.pasindu.nursingotapp.domain.usecase.PurgeOldIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.SetClinicalTaskCompletedUseCase
import com.pasindu.nursingotapp.domain.usecase.UpdateClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.UpdateIsbarNoteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ClinicalPlanningUseCaseModule {
    @Provides fun provideObserveIsbarNotesUseCase(dao: ClinicalPlanningDao) = ObserveIsbarNotesUseCase(dao)
    @Provides fun provideObserveClinicalTasksUseCase(dao: ClinicalPlanningDao) = ObserveClinicalTasksUseCase(dao)
    @Provides fun provideAddIsbarNoteUseCase(dao: ClinicalPlanningDao) = AddIsbarNoteUseCase(dao)
    @Provides fun provideUpdateIsbarNoteUseCase(dao: ClinicalPlanningDao) = UpdateIsbarNoteUseCase(dao)
    @Provides fun provideDeleteIsbarNoteUseCase(dao: ClinicalPlanningDao) = DeleteIsbarNoteUseCase(dao)
    @Provides fun providePurgeOldIsbarNotesUseCase(dao: ClinicalPlanningDao) = PurgeOldIsbarNotesUseCase(dao)
    @Provides fun provideAddClinicalTaskUseCase(dao: ClinicalPlanningDao) = AddClinicalTaskUseCase(dao)
    @Provides fun provideUpdateClinicalTaskUseCase(dao: ClinicalPlanningDao) = UpdateClinicalTaskUseCase(dao)
    @Provides fun provideDeleteClinicalTaskUseCase(dao: ClinicalPlanningDao) = DeleteClinicalTaskUseCase(dao)
    @Provides fun provideSetClinicalTaskCompletedUseCase(dao: ClinicalPlanningDao) = SetClinicalTaskCompletedUseCase(dao)
    @Provides fun provideObserveShiftWorkloadUseCase(dailyEntryDao: DailyEntryDao, claimPeriodDao: ClaimPeriodDao) =
        ObserveShiftWorkloadUseCase(dailyEntryDao, claimPeriodDao)
}
