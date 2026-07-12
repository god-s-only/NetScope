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
        description = "A network inspector built for Android developers.",
        bulletPoints = listOf(
            "See every HTTP request your app makes",
            "Full request and response bodies",
            "Works on WiFi and mobile data",
            "No root access required",
        ),
    ),
    HOW_IT_WORKS(
        title = "One dependency away",
        description = "Add NetScope to your app in three steps.",
        bulletPoints = listOf(
            "Add the interceptor to your build.gradle",
            "Attach it to your OkHttpClient",
            "Add network_security_config.xml for HTTPS",
            "Install NetScope on the same device",
        ),
    ),
    WHAT_YOU_GET(
        title = "Debug faster",
        description = "Everything you need to understand your app's network layer.",
        bulletPoints = listOf(
            "HTTP traffic list with search and filters",
            "Replay any request with modified headers or body",
            "Timeline waterfall showing request concurrency",
            "Export to HAR — open in Chrome DevTools or Postman",
        ),
    ),
}