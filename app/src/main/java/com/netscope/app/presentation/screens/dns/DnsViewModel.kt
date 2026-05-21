package com.netscope.app.presentation.screens.dns

import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.usecase.ObserveDnsEntriesUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DnsViewModel @Inject constructor(
    private val observeDnsEntriesUseCase: ObserveDnsEntriesUseCase,
) : BaseViewModel<DnsUiState>(DnsUiState()) {

    private val searchFlow = MutableStateFlow("")

    init {
        observeDnsEntries()
    }

    private fun observeDnsEntries() {
        searchFlow
            .debounce(300L)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                observeDnsEntriesUseCase(query)
            }
            .onEach { entries ->
                updateState {
                    copy(
                        entries    = entries,
                        totalCount = entries.size,
                        error      = null,
                    )
                }
            }
            .catch { e -> updateState { copy(error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        searchFlow.value = query
        updateState { copy(searchQuery = query) }
    }

    fun clearSearch() {
        searchFlow.value = ""
        updateState { copy(searchQuery = "") }
    }

    fun dismissError() = updateState { copy(error = null) }
}