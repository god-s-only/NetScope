package com.netscope.app.presentation.screens.replay

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
class ReplayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHttpTransactionDetailUseCase: GetHttpTransactionDetailUseCase,
    private val replayRequestUseCase: ReplayRequestUseCase,
) : BaseViewModel<ReplayUiState>(ReplayUiState()) {

    private val transactionId: String =
        checkNotNull(savedStateHandle[NavArgs.TRANSACTION_ID])

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            val transaction = getHttpTransactionDetailUseCase(transactionId)
            updateState {
                copy(
                    originalTransaction = transaction,
                    editableUrl = transaction?.url ?: "",
                    editableHeaders = transaction?.requestHeaders ?: emptyMap(),
                    editableBody = transaction?.requestBody ?: "",
                    isLoading = false,
                    error = if (transaction == null) "Transaction not found" else null,
                )
            }
        }
    }

    fun onUrlChanged(url: String) =
        updateState { copy(editableUrl = url) }

    fun onBodyChanged(body: String) =
        updateState { copy(editableBody = body) }

    fun onHeaderAdded(key: String, value: String) {
        val updated = state.editableHeaders.toMutableMap()
        updated[key] = value
        updateState { copy(editableHeaders = updated) }
    }

    fun onHeaderRemoved(key: String) {
        val updated = state.editableHeaders.toMutableMap()
        updated.remove(key)
        updateState { copy(editableHeaders = updated) }
    }

    fun onHeaderEdited(oldKey: String, newKey: String, newValue: String) {
        val updated = state.editableHeaders.toMutableMap()
        updated.remove(oldKey)
        updated[newKey] = newValue
        updateState { copy(editableHeaders = updated) }
    }

    fun resetToOriginal() {
        val tx = state.originalTransaction ?: return
        updateState {
            copy(
                editableUrl = tx.url,
                editableHeaders = tx.requestHeaders,
                editableBody = tx.requestBody ?: "",
                replayResult = null,
                error = null,
            )
        }
    }

    fun sendReplay() {
        val transaction = state.originalTransaction ?: return
        viewModelScope.launch {
            updateState { copy(isReplaying = true, error = null) }
            val config = ReplayRequestUseCase.ReplayConfig(
                transaction = transaction,
                overrideHeaders = state.editableHeaders,
                overrideBody = state.editableBody.takeIf { it.isNotBlank() },
                overrideUrl = state.editableUrl.takeIf { it != transaction.url },
            )
            when (val result = replayRequestUseCase(config)) {
                is ReplayRequestUseCase.ReplayResult.Success ->
                    updateState {
                        copy(
                            isReplaying = false,
                            replayResult = result.transaction,
                        )
                    }
                is ReplayRequestUseCase.ReplayResult.Failure ->
                    updateState {
                        copy(
                            isReplaying = false,
                            error = result.error,
                        )
                    }
            }
        }
    }

    fun clearResult() = updateState { copy(replayResult = null) }
    fun dismissError() = updateState { copy(error = null) }
}