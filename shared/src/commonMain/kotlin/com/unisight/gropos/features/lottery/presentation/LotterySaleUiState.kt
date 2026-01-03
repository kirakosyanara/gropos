package com.unisight.gropos.features.lottery.presentation

import androidx.compose.runtime.Immutable
import com.unisight.gropos.features.lottery.domain.model.LotteryGame
import com.unisight.gropos.features.lottery.domain.model.LotteryGameType
import java.math.BigDecimal

/**
 * UI State for the Lottery Sale screen.
 * 
 * **Per LOTTERY_SALES.md:**
 * - Display active games organized by type
 * - Shopping cart for lottery tickets
 * - Total calculation
 * 
 * Per kotlin-compose.mdc: All data classes in Composable params must be @Immutable.
 */
@Immutable
data class LotterySaleUiState(
    /** All active lottery games from repository. */
    val activeGames: List<LotteryGame> = emptyList(),
    
    /** Games filtered by current filter (or all if no filter). */
    val filteredGames: List<LotteryGame> = emptyList(),
    
    /** Current filter type (null = show all). */
    val selectedFilter: LotteryGameType? = null,
    
    /** Cart items for current sale. */
    val cart: List<LotteryCartItem> = emptyList(),
    
    /** Total amount due for cart. */
    val totalDue: BigDecimal = BigDecimal.ZERO,
    
    /** Loading state. */
    val isLoading: Boolean = false,
    
    /** Processing sale state. */
    val isProcessing: Boolean = false,
    
    /** Error message to display. */
    val errorMessage: String? = null,
    
    /** Success message after sale. */
    val successMessage: String? = null
) {
    /**
     * Whether the cart has items and can process sale.
     */
    val canProcessSale: Boolean
        get() = cart.isNotEmpty() && !isProcessing
    
    /**
     * Number of items in cart.
     */
    val cartItemCount: Int
        get() = cart.sumOf { it.quantity }
    
    /**
     * Grouped games by type for UI display.
     */
    val gamesByType: Map<LotteryGameType, List<LotteryGame>>
        get() = filteredGames.groupBy { it.type }
}

/**
 * Represents an item in the lottery sale cart.
 * 
 * @property gameId Unique identifier of the game
 * @property gameName Display name
 * @property ticketPrice Price per ticket
 * @property quantity Number of tickets
 * @property type Game type (scratcher or draw)
 */
@Immutable
data class LotteryCartItem(
    val gameId: String,
    val gameName: String,
    val ticketPrice: BigDecimal,
    val quantity: Int,
    val type: LotteryGameType
) {
    /**
     * Line total = ticketPrice × quantity.
     */
    val lineTotal: BigDecimal
        get() = ticketPrice.multiply(BigDecimal(quantity))
}

