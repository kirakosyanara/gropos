package com.unisight.gropos.features.lottery.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.features.lottery.domain.repository.LotteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Lottery Report screen.
 * 
 * **Per LOTTERY_REPORTS.md:**
 * - Fetch daily summary from repository
 * - Display sales vs. payouts breakdown
 * - Calculate net cash position
 * 
 * Per agent-behavior.mdc: No business logic in Composables.
 * Per kotlin-compose.mdc: Expose StateFlow (not LiveData).
 */
class LotteryReportViewModel(
    private val repository: LotteryRepository
) : ScreenModel {
    
    private val _uiState = MutableStateFlow(LotteryReportUiState())
    val uiState: StateFlow<LotteryReportUiState> = _uiState.asStateFlow()
    
    init {
        loadReport()
    }
    
    /**
     * Loads the daily lottery report.
     */
    private fun loadReport() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Fetch summary
                val summary = repository.getCurrentDaySummary()
                
                _uiState.update { state ->
                    state.copy(
                        summary = summary,
                        isLoading = false
                    )
                }
                
                // Also collect transactions
                repository.getTodaysTransactions().collect { transactions ->
                    _uiState.update { state ->
                        state.copy(transactions = transactions)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load report"
                    )
                }
            }
        }
    }
    
    /**
     * Refreshes the report data.
     */
    fun refresh() {
        loadReport()
    }
    
    /**
     * Dismisses the error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

