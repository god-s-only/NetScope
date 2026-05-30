package com.netscope.app.presentation.screens.connections

import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.repository.ConnectionRepository
import com.netscope.app.domain.usecase.ObserveConnectionsUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionsUiState(
    val allConnections: List<ConnectionEntry> = emptyList(),
    val activeConnections: List<ConnectionEntry> = emptyList(),
    val flaggedConnections: List<ConnectionEntry> = emptyList(),
    val activeCount: Int = 0,
    val selectedTab: ConnectionTab = ConnectionTab.ALL,
    val showClearConfirmDialog: Boolean = false,
    val error: String? = null,
)

enum class ConnectionTab { ALL, FLAGGED }

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val observeConnectionsUseCase: ObserveConnectionsUseCase,
    private val connectionRepository: ConnectionRepository,
) : BaseViewModel<ConnectionsUiState>(ConnectionsUiState()) {

    init {
        observeConnections()
    }

    private fun observeConnections() {
        observeConnectionsUseCase()
            .onEach { connectionState ->
                updateState {
                    copy(
                        allConnections = connectionState.all,
                        activeConnections = connectionState.active,
                        flaggedConnections = connectionState.flagged,
                        activeCount = connectionState.activeCount,
                        error = null,
                    )
                }
            }
            .catch { e -> updateState { copy(error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun onTabSelected(tab: ConnectionTab) =
        updateState { copy(selectedTab = tab) }

    fun onClearTapped() =
        updateState { copy(showClearConfirmDialog = true) }

    fun onClearConfirmed() {
        viewModelScope.launch {
            connectionRepository.clearAll()
            updateState { copy(showClearConfirmDialog = false) }
        }
    }

    fun onClearDismissed() =
        updateState { copy(showClearConfirmDialog = false) }

    fun dismissError() = updateState { copy(error = null) }
}