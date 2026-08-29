package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.data.local.AppDatabase
import com.pasindu.nursingotapp.data.repository.NurseCommandCenterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNurseCommandCenterRepository(
        database: AppDatabase
    ): NurseCommandCenterRepository = NurseCommandCenterRepository(database)
}
