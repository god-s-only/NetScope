package com.netscope.app.presentation.screens.connections

import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.usecase.ObserveConnectionsUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val observeConnectionsUseCase: ObserveConnectionsUseCase,
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

    fun dismissError() = updateState { copy(error = null) }
}