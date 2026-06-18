package com.netscope.app.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val isCompleted: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNextPage() {
        val current = _uiState.value.currentPage
        if (current < OnboardingPage.entries.size - 1) {
            _uiState.value = _uiState.value.copy(currentPage = current + 1)
        }
    }

    fun onPreviousPage() {
        val current = _uiState.value.currentPage
        if (current > 0) {
            _uiState.value = _uiState.value.copy(currentPage = current - 1)
        }
    }

    fun onGetStarted() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted()
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }
}

enum class OnboardingPage(
    val title: String,
    val description: String,
    val bulletPoints: List<String>,
) {
    WELCOME(
        title = "Welcome to NetScope",
        description = "A professional network inspector for Android developers.",
        bulletPoints = listOf(
            "Capture HTTP and HTTPS traffic from any app",
            "See full request and response bodies",
            "No root access required",
        ),
    ),
    HOW_IT_WORKS(
        title = "How it works",
        description = "NetScope runs a local proxy on your device.",
        bulletPoints = listOf(
            "Install the CA certificate once",
            "Set your WiFi proxy to 127.0.0.1:8888",
            "Every app's traffic flows through NetScope",
            "Browse, inspect, replay — all on device",
        ),
    ),
    WHAT_YOU_GET(
        title = "What you get",
        description = "Everything you need to debug network issues.",
        bulletPoints = listOf(
            "HTTP Traffic list with full request and response",
            "Request replay with editable headers and body",
            "DNS log, connections tracker, timeline chart",
            "Export to HAR — open in Chrome DevTools or Postman",
        ),
    ),
}