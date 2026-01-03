package com.unisight.gropos.features.lottery.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.features.cashier.domain.service.CashierSessionManager
import com.unisight.gropos.features.lottery.domain.model.LotteryTransaction
import com.unisight.gropos.features.lottery.domain.repository.LotteryRepository
import com.unisight.gropos.features.lottery.domain.service.PayoutTierCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * ViewModel for the Lottery Payout screen.
 * 
 * **Per LOTTERY_PAYOUTS.md:**
 * - Accept payout amount via numeric keypad
 * - Validate against tier limits
 * - Process payout if allowed
 * - Show rejection for Tier 3 ($600+)
 * 
 * **Tier Logic (via PayoutTierCalculator):**
 * - Tier 1 ($0-$49.99): APPROVED
 * - Tier 2 ($50-$599.99): LOGGED_ONLY
 * - Tier 3 ($600+): REJECTED_OVER_LIMIT
 * 
 * **P0 FIX (QA Audit):**
 * - staffId now comes from CashierSessionManager (not hardcoded)
 * - Throws IllegalStateException if no active session
 * 
 * Per agent-behavior.mdc: No business logic in Composables.
 * Per kotlin-compose.mdc: Expose StateFlow (not LiveData).
 */
class LotteryPayoutViewModel(
    private val repository: LotteryRepository,
    private val sessionManager: CashierSessionManager
) : ScreenModel {
    
    /**
     * Gets the current staff ID from the active session.
     * 
     * @throws IllegalStateException if no session is active
     */
    private val staffId: Int
        get() = sessionManager.activeSession.value?.employeeId
            ?: throw IllegalStateException("No active session. User must be logged in to process lottery payouts.")
    
    private val _uiState = MutableStateFlow(LotteryPayoutUiState())
    val uiState: StateFlow<LotteryPayoutUiState> = _uiState.asStateFlow()
    
    // Internal cents accumulator for keypad input
    private var centsInput = StringBuilder()
    
    // ========================================================================
    // Keypad Input
    // ========================================================================
    
    /**
     * Handles digit press on the numeric keypad.
     * 
     * Amount is entered as cents (e.g., "2500" = $25.00).
     * 
     * @param digit The digit pressed (0-9)
     */
    fun onDigitPress(digit: String) {
        // Limit input length to prevent overflow
        if (centsInput.length >= 8) return
        
        // Ignore leading zeros unless it's the only digit
        if (centsInput.isEmpty() && digit == "0") {
            updateDisplay()
            return
        }
        
        centsInput.append(digit)
        updateDisplay()
    }
    
    /**
     * Clears the entered amount.
     */
    fun onClear() {
        centsInput.clear()
        updateDisplay()
    }
    
    /**
     * Removes the last entered digit.
     */
    fun onBackspace() {
        if (centsInput.isNotEmpty()) {
            centsInput.deleteCharAt(centsInput.length - 1)
        }
        updateDisplay()
    }
    
    /**
     * Updates the display and validates the amount.
     */
    private fun updateDisplay() {
        val cents = centsInput.toString().toLongOrNull() ?: 0
        val dollars = BigDecimal(cents)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        
        // Validate against tier calculator
        val validationResult = if (dollars > BigDecimal.ZERO) {
            PayoutTierCalculator.validatePayout(dollars)
        } else {
            null
        }
        
        _uiState.update { state ->
            state.copy(
                amount = dollars,
                displayAmount = centsInput.toString().ifEmpty { "0" },
                validationResult = validationResult,
                errorMessage = null
            )
        }
    }
    
    // ========================================================================
    // Payout Processing
    // ========================================================================
    
    /**
     * Processes the current payout amount.
     * 
     * **CRITICAL:** Will fail for Tier 3 ($600+).
     * 
     * @return Result containing the transaction or error
     */
    suspend fun processPayout(): Result<LotteryTransaction> {
        val currentAmount = _uiState.value.amount
        
        if (currentAmount <= BigDecimal.ZERO) {
            return Result.failure(IllegalStateException("Amount must be greater than zero"))
        }
        
        val validation = _uiState.value.validationResult
        if (validation == null || !validation.canProcess) {
            val message = validation?.message ?: "Invalid amount"
            _uiState.update { it.copy(errorMessage = message) }
            return Result.failure(IllegalStateException(message))
        }
        
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        
        val result = repository.processPayout(
            amount = currentAmount,
            gameId = null, // Generic payout
            staffId = staffId
        )
        
        result.fold(
            onSuccess = { transaction ->
                // Reset for next payout
                centsInput.clear()
                _uiState.update { state ->
                    state.copy(
                        amount = BigDecimal.ZERO,
                        displayAmount = "0",
                        validationResult = null,
                        isProcessing = false,
                        successMessage = "Payout processed: \$${transaction.amount}"
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        errorMessage = error.message ?: "Payout failed"
                    )
                }
            }
        )
        
        return result
    }
    
    /**
     * Dismisses the error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Dismisses the success message.
     */
    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

