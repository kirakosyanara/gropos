package com.unisight.gropos.features.lottery.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.features.lottery.domain.model.LotteryGame
import com.unisight.gropos.features.lottery.domain.model.LotteryGameType
import com.unisight.gropos.features.lottery.domain.model.LotteryTransaction
import com.unisight.gropos.features.lottery.domain.repository.LotteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for the Lottery Sale screen.
 * 
 * **Per LOTTERY_SALES.md:**
 * - Load active lottery games
 * - Manage cart (add, remove, update quantity)
 * - Calculate total
 * - Process sale transactions
 * 
 * **Business Rules:**
 * - Cash-only transactions
 * - Each game purchase becomes a separate transaction
 * - Cart is cleared after successful sale
 * 
 * Per agent-behavior.mdc: No business logic in Composables.
 * Per kotlin-compose.mdc: Expose StateFlow (not LiveData).
 */
class LotterySaleViewModel(
    private val repository: LotteryRepository,
    private val staffId: Int
) : ScreenModel {
    
    private val _uiState = MutableStateFlow(LotterySaleUiState(isLoading = true))
    val uiState: StateFlow<LotterySaleUiState> = _uiState.asStateFlow()
    
    init {
        loadGames()
    }
    
    // ========================================================================
    // Data Loading
    // ========================================================================
    
    /**
     * Loads active lottery games from repository.
     */
    private fun loadGames() {
        screenModelScope.launch {
            repository.getActiveGames().collect { games ->
                _uiState.update { state ->
                    state.copy(
                        activeGames = games,
                        filteredGames = applyFilter(games, state.selectedFilter),
                        isLoading = false
                    )
                }
            }
        }
    }
    
    // ========================================================================
    // Cart Operations
    // ========================================================================
    
    /**
     * Adds a game to the cart.
     * 
     * If the game already exists in cart, increments quantity.
     * 
     * @param game The lottery game to add
     * @param quantity Number of tickets (default 1)
     */
    fun addToCart(game: LotteryGame, quantity: Int = 1) {
        if (quantity < 1) return
        
        _uiState.update { state ->
            val existingIndex = state.cart.indexOfFirst { it.gameId == game.id }
            
            val newCart = if (existingIndex >= 0) {
                // Increment existing item
                state.cart.mapIndexed { index, item ->
                    if (index == existingIndex) {
                        item.copy(quantity = item.quantity + quantity)
                    } else {
                        item
                    }
                }
            } else {
                // Add new item
                state.cart + LotteryCartItem(
                    gameId = game.id,
                    gameName = game.name,
                    ticketPrice = game.ticketPrice,
                    quantity = quantity,
                    type = game.type
                )
            }
            
            state.copy(
                cart = newCart,
                totalDue = calculateTotal(newCart)
            )
        }
    }
    
    /**
     * Removes a game from the cart by ID.
     * 
     * @param gameId The game ID to remove
     */
    fun removeFromCart(gameId: String) {
        _uiState.update { state ->
            val newCart = state.cart.filterNot { it.gameId == gameId }
            state.copy(
                cart = newCart,
                totalDue = calculateTotal(newCart)
            )
        }
    }
    
    /**
     * Updates the quantity of a cart item.
     * 
     * If newQuantity is 0 or less, removes the item.
     * 
     * @param gameId The game ID to update
     * @param newQuantity The new quantity
     */
    fun updateQuantity(gameId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(gameId)
            return
        }
        
        _uiState.update { state ->
            val newCart = state.cart.map { item ->
                if (item.gameId == gameId) {
                    item.copy(quantity = newQuantity)
                } else {
                    item
                }
            }
            
            state.copy(
                cart = newCart,
                totalDue = calculateTotal(newCart)
            )
        }
    }
    
    /**
     * Clears all items from the cart.
     */
    fun clearCart() {
        _uiState.update { state ->
            state.copy(
                cart = emptyList(),
                totalDue = BigDecimal.ZERO
            )
        }
    }
    
    // ========================================================================
    // Filtering
    // ========================================================================
    
    /**
     * Filters games by type.
     * 
     * @param type The game type to filter by (SCRATCHER or DRAW)
     */
    fun filterByType(type: LotteryGameType) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = type,
                filteredGames = applyFilter(state.activeGames, type)
            )
        }
    }
    
    /**
     * Clears the filter to show all games.
     */
    fun clearFilter() {
        _uiState.update { state ->
            state.copy(
                selectedFilter = null,
                filteredGames = state.activeGames
            )
        }
    }
    
    private fun applyFilter(games: List<LotteryGame>, filter: LotteryGameType?): List<LotteryGame> {
        return if (filter == null) {
            games
        } else {
            games.filter { it.type == filter }
        }
    }
    
    // ========================================================================
    // Sale Processing
    // ========================================================================
    
    /**
     * Processes the current cart as a sale.
     * 
     * Each cart item becomes a separate transaction in the repository.
     * Cart is cleared on success.
     * 
     * @return Result indicating success or failure
     */
    suspend fun processSale(): Result<List<LotteryTransaction>> {
        val currentCart = _uiState.value.cart
        
        if (currentCart.isEmpty()) {
            return Result.failure(IllegalStateException("Cart is empty"))
        }
        
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        
        val transactions = mutableListOf<LotteryTransaction>()
        
        for (item in currentCart) {
            val result = repository.recordSale(
                gameId = item.gameId,
                quantity = item.quantity,
                staffId = staffId
            )
            
            if (result.isFailure) {
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Sale failed"
                    )
                }
                return Result.failure(result.exceptionOrNull() ?: Exception("Sale failed"))
            }
            
            transactions.add(result.getOrThrow())
        }
        
        // Clear cart and show success
        val total = _uiState.value.totalDue
        _uiState.update { state ->
            state.copy(
                cart = emptyList(),
                totalDue = BigDecimal.ZERO,
                isProcessing = false,
                successMessage = "Sale completed: $$total"
            )
        }
        
        return Result.success(transactions)
    }
    
    /**
     * Dismisses the current error message.
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
    
    // ========================================================================
    // Helpers
    // ========================================================================
    
    private fun calculateTotal(cart: List<LotteryCartItem>): BigDecimal {
        return cart.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.lineTotal)
        }
    }
}

