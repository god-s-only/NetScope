package com.netscope.app.domain.repository

import com.netscope.app.domain.model.BandwidthSnapshot
import kotlinx.coroutines.flow.Flow

interface BandwidthRepository {
    fun observeBandwidthSnapshots(): Flow<List<BandwidthSnapshot>>
    fun observePerAppBandwidth(): Flow<Map<String, BandwidthSnapshot>>
}