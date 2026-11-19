package com.pasindu.nursingotapp.statgraph.di

import com.pasindu.nursingotapp.statgraph.data.repository.RoomStatDatasetRepository
import com.pasindu.nursingotapp.statgraph.data.repository.StatDatasetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StatGraphModule {

    @Binds
    @Singleton
    abstract fun bindStatDatasetRepository(
        impl: RoomStatDatasetRepository
    ): StatDatasetRepository
}
