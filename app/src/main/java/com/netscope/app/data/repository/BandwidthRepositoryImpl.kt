package com.netscope.app.data.repository

import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.repository.BandwidthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BandwidthRepositoryImpl @Inject constructor(): BandwidthRepository {
    override fun observeBandwidthSnapshots(): Flow<List<BandwidthSnapshot>> {
        return flow { emit(emptyList()) }
    }

    override fun observePerAppBandwidth(): Flow<Map<String, BandwidthSnapshot>> {
        return flow { emit(emptyMap()) }

    }
}