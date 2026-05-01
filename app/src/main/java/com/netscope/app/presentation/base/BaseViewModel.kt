package com.netscope.app.presentation.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<S>(initialState: S) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    protected var state: S
        get() = _uiState.value
        set(value) { _uiState.value = value }

    protected fun updateState(update: S.() -> S) {
        _uiState.value = _uiState.value.update()
    }
}