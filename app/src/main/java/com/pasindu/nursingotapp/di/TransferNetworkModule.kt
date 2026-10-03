package com.pasindu.nursingotapp.di

import com.pasindu.nursingotapp.transfer.data.FirebaseAuthTokenProvider
import com.pasindu.nursingotapp.transfer.data.FirestoreTransferRemoteDataSource
import com.pasindu.nursingotapp.transfer.data.HttpTransferWorkerApiClient
import com.pasindu.nursingotapp.transfer.data.TransferRemoteDataSource
import com.pasindu.nursingotapp.transfer.data.TransferTokenProvider
import com.pasindu.nursingotapp.transfer.data.TransferWorkerApiClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TransferNetworkBindingModule {

    @Binds
    @Singleton
    abstract fun bindTransferTokenProvider(
        impl: FirebaseAuthTokenProvider
    ): TransferTokenProvider

    @Binds
    @Singleton
    abstract fun bindTransferRemoteDataSource(
        impl: FirestoreTransferRemoteDataSource
    ): TransferRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTransferWorkerApiClient(
        impl: HttpTransferWorkerApiClient
    ): TransferWorkerApiClient

    @Binds
    @Singleton
    abstract fun bindTransferSyncScheduler(
        impl: com.pasindu.nursingotapp.transfer.worker.WorkManagerTransferSyncScheduler
    ): com.pasindu.nursingotapp.transfer.worker.TransferSyncScheduler
}

@Module
@InstallIn(SingletonComponent::class)
object TransferNetworkConfigModule {

    @Provides
    @Singleton
    @Named("transfer_worker_base_url")
    fun provideTransferWorkerBaseUrl(): String {
        // Can be configured per build flavor or default Cloudflare Worker endpoint
        return "https://nursing-transfer-worker.pasindu-apps.workers.dev"
    }
}
