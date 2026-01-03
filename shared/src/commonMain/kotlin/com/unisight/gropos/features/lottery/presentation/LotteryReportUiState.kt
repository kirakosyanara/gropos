package com.unisight.gropos.features.lottery.presentation

import androidx.compose.runtime.Immutable
import com.unisight.gropos.features.lottery.domain.model.LotteryDailySummary
import com.unisight.gropos.features.lottery.domain.model.LotteryTransaction
import java.math.BigDecimal

/**
 * UI State for the Lottery Report screen.
 * 
 * **Per LOTTERY_REPORTS.md:**
 * - Display daily sales total
 * - Display daily payouts total
 * - Calculate net cash (Sales - Payouts)
 * - Show transaction breakdown
 * 
 * Per kotlin-compose.mdc: All data classes in Composable params must be @Immutable.
 */
@Immutable
data class LotteryReportUiState(
    /** Current day's summary. */
    val summary: LotteryDailySummary? = null,
    
    /** Today's transactions. */
    val transactions: List<LotteryTransaction> = emptyList(),
    
    /** Loading state. */
    val isLoading: Boolean = true,
    
    /** Error message to display. */
    val errorMessage: String? = null
) {
    /**
     * Net cash (Sales - Payouts).
     */
    val netCash: BigDecimal
        get() = summary?.netAmount ?: BigDecimal.ZERO
    
    /**
     * Total sales.
     */
    val totalSales: BigDecimal
        get() = summary?.totalSales ?: BigDecimal.ZERO
    
    /**
     * Total payouts.
     */
    val totalPayouts: BigDecimal
        get() = summary?.totalPayouts ?: BigDecimal.ZERO
    
    /**
     * Transaction count.
     */
    val transactionCount: Int
        get() = summary?.transactionCount ?: 0
    
    /**
     * Whether net is positive (good).
     */
    val isPositiveNet: Boolean
        get() = netCash >= BigDecimal.ZERO
}

