package com.netscope.app.di

import com.netscope.app.data.repository.BandwidthRepositoryImpl
import com.netscope.app.data.repository.ConnectionRepositoryImpl
import com.netscope.app.data.repository.DnsRepositoryImpl
import com.netscope.app.data.repository.TrafficRepositoryImpl
import com.netscope.app.domain.repository.BandwidthRepository
import com.netscope.app.domain.repository.ConnectionRepository
import com.netscope.app.domain.repository.DnsRepository
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

    @Binds
    @Singleton
    abstract fun bindDnsRepository(
        impl: DnsRepositoryImpl,
    ): DnsRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl,
    ): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindBandwidthRepository(
        impl: BandwidthRepositoryImpl,
    ): BandwidthRepository
}