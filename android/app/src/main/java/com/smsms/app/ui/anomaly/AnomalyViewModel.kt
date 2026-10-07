package com.smsms.app.ui.anomaly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsms.app.model.AnomalyFlag
import com.smsms.app.repository.OrdersRepository
import com.smsms.app.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AnomalyUiState {
    data object Loading : AnomalyUiState()
    data class Loaded(
        // Every flag from the last run (normal and outlier alike), worst first.
        val flags: List<AnomalyFlag>,
        val running: Boolean = false,
        val runError: String? = null
    ) : AnomalyUiState() {
        val outliers: List<AnomalyFlag> get() = flags.filter { it.is_outlier }
    }
    data class Error(val message: String) : AnomalyUiState()
}

class AnomalyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AnomalyUiState>(AnomalyUiState.Loading)
    val uiState: StateFlow<AnomalyUiState> = _uiState.asStateFlow()

    init {
        loadFlags()
    }

    fun loadFlags() {
        viewModelScope.launch {
            _uiState.value = AnomalyUiState.Loading
            when (val result = OrdersRepository.listAnomalyFlags()) {
                is ApiResult.Success -> _uiState.value = AnomalyUiState.Loaded(result.data)
                is ApiResult.Failure -> _uiState.value = AnomalyUiState.Error(result.message)
            }
        }
    }

    // Re-runs the z-score script on the backend over all current quality
    // logs, then reloads the flags it just wrote.
    fun runDetection() {
        val current = _uiState.value as? AnomalyUiState.Loaded ?: return
        _uiState.value = current.copy(running = true, runError = null)
        viewModelScope.launch {
            when (val result = OrdersRepository.runAnomalyDetection()) {
                is ApiResult.Success -> loadFlags()
                is ApiResult.Failure -> {
                    val loaded = _uiState.value as? AnomalyUiState.Loaded ?: return@launch
                    _uiState.value = loaded.copy(running = false, runError = result.message)
                }
            }
        }
    }
}
