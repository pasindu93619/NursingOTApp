package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.domain.usecase.AddClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.AddIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClinicalTasksUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.PurgeOldIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.SetClinicalTaskCompletedUseCase
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
    @Provides fun providePurgeOldIsbarNotesUseCase(dao: ClinicalPlanningDao) = PurgeOldIsbarNotesUseCase(dao)
    @Provides fun provideAddClinicalTaskUseCase(dao: ClinicalPlanningDao) = AddClinicalTaskUseCase(dao)
    @Provides fun provideSetClinicalTaskCompletedUseCase(dao: ClinicalPlanningDao) = SetClinicalTaskCompletedUseCase(dao)
}
