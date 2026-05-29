package com.netscope.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.netscope.app.domain.model.AppSettings
import com.netscope.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "netscope_settings",
)

private const val TAG = "SettingsRepositoryImpl"

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val MAX_STORED_REQUESTS = intPreferencesKey("max_stored_requests")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll_traffic_list")
        val SHOW_REPLAYED = booleanPreferencesKey("show_replayed_requests")
    }

    override fun observeSettings(): Flow<AppSettings> =
        context.dataStore.data
            .catch { e ->
                Log.e(TAG, "DataStore error: ${e.message}")
                emit(androidx.datastore.preferences.core.emptyPreferences())
            }
            .map { prefs ->
                AppSettings(
                    maxStoredRequests = prefs[Keys.MAX_STORED_REQUESTS] ?: 500,
                    autoScrollTrafficList = prefs[Keys.AUTO_SCROLL] ?: true,
                    showReplayedRequests = prefs[Keys.SHOW_REPLAYED] ?: true,
                )
            }

    override suspend fun setMaxStoredRequests(max: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_STORED_REQUESTS] = max
        }
        Log.d(TAG, "maxStoredRequests set to $max")
    }

    override suspend fun setAutoScrollTrafficList(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SCROLL] = enabled
        }
    }

    override suspend fun setShowReplayedRequests(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_REPLAYED] = enabled
        }
    }
}