package com.smsms.app.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsms.app.model.SalesForecast
import com.smsms.app.repository.OrdersRepository
import com.smsms.app.util.ApiResult
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ForecastUiState {
    data object Loading : ForecastUiState()
    data class Loaded(
        val rows: List<SalesForecast>,
        val mae: Double?,
        val rmse: Double?,
        val modelUsed: String?,
        val running: Boolean = false,
        val runError: String? = null,
        // Month the Plant Head should enter next ("YYYY-MM-DD"), or null if
        // none can be entered right now (month not finished / status failed).
        val nextMonth: String? = null,
        val addActualOpen: Boolean = false,
        val savingActual: Boolean = false,
        val actualError: String? = null
    ) : ForecastUiState()
    data class Error(val message: String) : ForecastUiState()
}

class ForecastViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    init {
        loadForecast()
    }

    fun loadForecast() {
        viewModelScope.launch {
            _uiState.value = ForecastUiState.Loading
            when (val result = OrdersRepository.listSalesForecast()) {
                is ApiResult.Success -> {
                    // Failing to fetch the "next month" just hides the add
                    // button - it shouldn't block viewing the forecast itself.
                    val status = (OrdersRepository.getActualsStatus() as? ApiResult.Success)?.data
                    val nextMonth = status?.next_month?.takeIf { status.can_enter }
                    _uiState.value = buildLoadedState(result.data).copy(nextMonth = nextMonth)
                }
                is ApiResult.Failure -> _uiState.value = ForecastUiState.Error(result.message)
            }
        }
    }

    fun openAddActual() {
        val loaded = _uiState.value as? ForecastUiState.Loaded ?: return
        _uiState.value = loaded.copy(addActualOpen = true, actualError = null)
    }

    fun dismissAddActual() {
        val loaded = _uiState.value as? ForecastUiState.Loaded ?: return
        _uiState.value = loaded.copy(addActualOpen = false, actualError = null)
    }

    // Saves the real sales figure for `nextMonth`, then immediately re-runs
    // the forecast so the new month is used for training/evaluation and the
    // chart refreshes - one tap instead of "save" then "run forecast".
    fun addActual(amount: Double) {
        val loaded = _uiState.value as? ForecastUiState.Loaded ?: return
        val month = loaded.nextMonth ?: return
        _uiState.value = loaded.copy(savingActual = true, actualError = null)
        viewModelScope.launch {
            when (val result = OrdersRepository.addActual(month, amount)) {
                is ApiResult.Success -> {
                    val current = _uiState.value as? ForecastUiState.Loaded ?: return@launch
                    _uiState.value = current.copy(addActualOpen = false, savingActual = false, nextMonth = null)
                    runForecast()
                }
                is ApiResult.Failure -> {
                    val current = _uiState.value as? ForecastUiState.Loaded ?: return@launch
                    _uiState.value = current.copy(savingActual = false, actualError = result.message)
                }
            }
        }
    }

    // MAE/RMSE are computed here, not read from the backend, because
    // they're fully derivable from the rows we already have (predicted
    // vs actual, wherever actual_value isn't null) — same "don't
    // duplicate a value the client can derive" reasoning used throughout
    // this app.
    private fun buildLoadedState(rows: List<SalesForecast>): ForecastUiState.Loaded {
        val evaluated = rows.filter { it.actual_value != null }
        val errors = evaluated.map { row ->
            row.actual_value!!.toDouble() - row.predicted_value.toDouble()
        }
        val mae = if (errors.isNotEmpty()) errors.map { abs(it) }.average() else null
        val rmse = if (errors.isNotEmpty()) sqrt(errors.map { it * it }.average()) else null

        return ForecastUiState.Loaded(
            rows = rows,
            mae = mae,
            rmse = rmse,
            modelUsed = rows.firstOrNull()?.model_used
        )
    }

    fun runForecast() {
        val current = _uiState.value as? ForecastUiState.Loaded
        _uiState.value = (current ?: ForecastUiState.Loaded(emptyList(), null, null, null))
            .copy(running = true, runError = null)
        viewModelScope.launch {
            when (val result = OrdersRepository.runSalesForecast()) {
                is ApiResult.Success -> loadForecast()
                is ApiResult.Failure -> {
                    val loaded = _uiState.value as? ForecastUiState.Loaded ?: return@launch
                    _uiState.value = loaded.copy(running = false, runError = result.message)
                }
            }
        }
    }
}
