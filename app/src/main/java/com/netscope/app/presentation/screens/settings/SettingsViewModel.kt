package com.netscope.app.presentation.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.model.AppSettings
import com.netscope.app.domain.model.MaxRequestsOption
import com.netscope.app.domain.repository.SettingsRepository
import com.netscope.app.domain.usecase.ClearAllTrafficUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val appVersion: String = "",
    val packageName: String = "",
    val isCertInstalled: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
    val clearSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val clearAllTrafficUseCase: ClearAllTrafficUseCase,
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    private val prefs get() =
        context.getSharedPreferences("netscope_prefs", Context.MODE_PRIVATE)

    init {
        loadAppInfo()
        observeSettings()
    }

    private fun loadAppInfo() {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            updateState {
                copy(
                    appVersion = info.versionName ?: "1.0.0",
                    packageName = context.packageName,
                    isCertInstalled = prefs.getBoolean("cert_installed", false),
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            updateState { copy(appVersion = "1.0.0", packageName = context.packageName) }
        }
    }

    private fun observeSettings() {
        settingsRepository.observeSettings()
            .onEach { settings -> updateState { copy(settings = settings) } }
            .catch { e -> updateState { copy(error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun onMaxRequestsChanged(option: MaxRequestsOption) {
        viewModelScope.launch {
            settingsRepository.setMaxStoredRequests(option.value)
        }
    }

    fun onAutoScrollChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoScrollTrafficList(enabled)
        }
    }

    fun onShowReplayedChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowReplayedRequests(enabled)
        }
    }

    fun onClearDataTapped() =
        updateState { copy(showClearConfirmDialog = true) }

    fun onClearDataConfirmed() {
        viewModelScope.launch {
            clearAllTrafficUseCase()
            updateState {
                copy(
                    showClearConfirmDialog = false,
                    clearSuccess = true,
                )
            }
        }
    }

    fun onClearDataDismissed() =
        updateState { copy(showClearConfirmDialog = false) }

    fun onClearSuccessConsumed() =
        updateState { copy(clearSuccess = false) }

    fun dismissError() = updateState { copy(error = null) }
}