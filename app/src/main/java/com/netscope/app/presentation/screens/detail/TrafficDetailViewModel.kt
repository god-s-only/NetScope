package com.netscope.app.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.usecase.GetHttpTransactionDetailUseCase
import com.netscope.app.domain.usecase.ReplayRequestUseCase
import com.netscope.app.presentation.base.BaseViewModel
import com.netscope.app.presentation.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrafficDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHttpTransactionDetailUseCase: GetHttpTransactionDetailUseCase,
    private val replayRequestUseCase: ReplayRequestUseCase,
) : BaseViewModel<TrafficDetailUiState>(TrafficDetailUiState()) {

    private val transactionId: String =
        checkNotNull(savedStateHandle[NavArgs.TRANSACTION_ID])

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val transaction = getHttpTransactionDetailUseCase(transactionId)
            updateState {
                copy(
                    transaction = transaction,
                    isLoading = false,
                    error = if (transaction == null) "Transaction not found" else null,
                )
            }
        }
    }

    fun onTabSelected(tab: DetailTab) =
        updateState { copy(activeTab = tab) }

    fun replayRequest(
        overrideHeaders: Map<String, String> = emptyMap(),
        overrideBody: String? = null,
    ) {
        val transaction = state.transaction ?: return
        viewModelScope.launch {
            updateState { copy(isReplaying = true, replayError = null) }
            val config = ReplayRequestUseCase.ReplayConfig(
                transaction = transaction,
                overrideHeaders = overrideHeaders,
                overrideBody = overrideBody,
            )
            when (val result = replayRequestUseCase(config)) {
                is ReplayRequestUseCase.ReplayResult.Success -> {
                    updateState {
                        copy(
                            isReplaying = false,
                            replayResult = result.transaction,
                        )
                    }
                }
                is ReplayRequestUseCase.ReplayResult.Failure -> {
                    updateState {
                        copy(
                            isReplaying = false,
                            replayError = result.error,
                        )
                    }
                }
            }
        }
    }

    fun clearReplayResult() = updateState { copy(replayResult = null, replayError = null) }
    fun dismissError() = updateState { copy(error = null) }
}