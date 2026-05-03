package com.netscope.app.di

import com.netscope.app.data.repository.TrafficRepositoryImpl
import com.netscope.app.domain.repository.TrafficRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTrafficRepository(
        impl: TrafficRepositoryImpl,
    ): TrafficRepository

}