package com.netscope.app.domain.repository

import com.netscope.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setMaxStoredRequests(max: Int)
    suspend fun setAutoScrollTrafficList(enabled: Boolean)
    suspend fun setShowReplayedRequests(enabled: Boolean)
    suspend fun setOnboardingCompleted()
    suspend fun isOnboardingCompleted(): Boolean
}