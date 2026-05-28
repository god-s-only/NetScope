package com.netscope.app.presentation.screens.traffic

import androidx.lifecycle.viewModelScope
import com.netscope.app.data.export.ExportManager
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.StatusCategory
import com.netscope.app.domain.model.TrafficFilter
import com.netscope.app.domain.usecase.ClearAllTrafficUseCase
import com.netscope.app.domain.usecase.ExportTrafficUseCase
import com.netscope.app.domain.usecase.ObserveHttpTransactionsUseCase
import com.netscope.app.presentation.base.BaseViewModel
import android.content.Intent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrafficListViewModel @Inject constructor(
    private val observeHttpTransactionsUseCase: ObserveHttpTransactionsUseCase,
    private val clearAllTrafficUseCase: ClearAllTrafficUseCase,
    private val exportTrafficUseCase: ExportTrafficUseCase,
    private val exportManager: ExportManager,
) : BaseViewModel<TrafficListUiState>(TrafficListUiState()) {

    private val filterFlow = MutableStateFlow(TrafficFilter())

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        filterFlow
            .debounce(300L)
            .distinctUntilChanged()
            .flatMapLatest { filter ->
                observeHttpTransactionsUseCase(filter)
            }
            .onEach { transactions ->
                updateState {
                    copy(
                        transactions = transactions,
                        totalCount = transactions.size,
                        filteredCount = transactions.size,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { e -> updateState { copy(error = e.message, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        filterFlow.value = filterFlow.value.copy(searchQuery = query)
        updateState { copy(filter = filterFlow.value) }
    }

    fun onMethodFilterToggled(method: HttpMethod) {
        val current = filterFlow.value.methods.toMutableSet()
        if (method in current) current.remove(method) else current.add(method)
        filterFlow.value = filterFlow.value.copy(methods = current)
        updateState { copy(filter = filterFlow.value) }
    }

    fun onStatusFilterToggled(category: StatusCategory) {
        val current = filterFlow.value.statusCategories.toMutableSet()
        if (category in current) current.remove(category) else current.add(category)
        filterFlow.value = filterFlow.value.copy(statusCategories = current)
        updateState { copy(filter = filterFlow.value) }
    }

    fun onShowSlowOnlyToggled() {
        filterFlow.value = filterFlow.value.copy(
            showSlowOnly = !filterFlow.value.showSlowOnly
        )
        updateState { copy(filter = filterFlow.value) }
    }

    fun onShowErrorsOnlyToggled() {
        filterFlow.value = filterFlow.value.copy(
            showErrorsOnly = !filterFlow.value.showErrorsOnly
        )
        updateState { copy(filter = filterFlow.value) }
    }

    fun clearFilters() {
        filterFlow.value = TrafficFilter()
        updateState { copy(filter = TrafficFilter()) }
    }

    fun clearAllTraffic() {
        viewModelScope.launch {
            clearAllTrafficUseCase()
        }
    }

    fun exportTraffic() {
        viewModelScope.launch {
            updateState { copy(isExporting = true, error = null) }
            when (val result = exportTrafficUseCase()) {
                is ExportTrafficUseCase.ExportResult.Success -> {
                    val intent = exportManager.saveAndShare(
                        json = result.json,
                        fileName = result.fileName,
                    )
                    updateState {
                        copy(
                            isExporting = false,
                            exportIntent = intent,
                        )
                    }
                }
                is ExportTrafficUseCase.ExportResult.Failure -> {
                    updateState {
                        copy(
                            isExporting = false,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun onExportIntentConsumed() =
        updateState { copy(exportIntent = null) }

    fun dismissError() = updateState { copy(error = null) }
}