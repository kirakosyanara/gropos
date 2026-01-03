package com.unisight.gropos.features.lottery.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.core.util.UsdCurrencyFormatter
import com.unisight.gropos.features.lottery.domain.model.LotteryGame
import com.unisight.gropos.features.lottery.domain.model.LotteryGameType
import com.unisight.gropos.features.lottery.presentation.LotteryCartItem
import com.unisight.gropos.features.lottery.presentation.LotterySaleUiState
import com.unisight.gropos.features.lottery.presentation.LotterySaleViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.math.BigDecimal

// Currency formatter instance
private val currencyFormatter = UsdCurrencyFormatter()

/**
 * Lottery Sale Screen.
 * 
 * **Per LOTTERY_SALES.md:**
 * - Displays active lottery games in a grid
 * - Tap to add to cart
 * - Shows cart summary with total
 * - Process sale button
 * 
 * **Layout:**
 * - Left: Game grid with filter chips
 * - Right: Cart summary with checkout
 * 
 * Per ui-ux-guidelines.mdc: Touch-friendly 44dp minimum targets.
 */
class LotterySaleScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel: LotterySaleViewModel = koinInject()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        
        // Show success/error messages
        LaunchedEffect(uiState.successMessage) {
            uiState.successMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.dismissSuccess()
            }
        }
        
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.dismissError()
            }
        }
        
        LotterySaleContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onBackClick = { navigator.pop() },
            onGameClick = { game -> viewModel.addToCart(game) },
            onFilterSelect = { type -> viewModel.filterByType(type) },
            onFilterClear = { viewModel.clearFilter() },
            onQuantityChange = { gameId, qty -> viewModel.updateQuantity(gameId, qty) },
            onRemoveItem = { gameId -> viewModel.removeFromCart(gameId) },
            onClearCart = { viewModel.clearCart() },
            onProcessSale = {
                scope.launch {
                    viewModel.processSale()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotterySaleContent(
    uiState: LotterySaleUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onGameClick: (LotteryGame) -> Unit,
    onFilterSelect: (LotteryGameType) -> Unit,
    onFilterClear: () -> Unit,
    onQuantityChange: (gameId: String, newQuantity: Int) -> Unit,
    onRemoveItem: (gameId: String) -> Unit,
    onClearCart: () -> Unit,
    onProcessSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lottery Sales") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left: Game Selection Panel (70%)
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Filter chips
                LotteryFilterChips(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelect = onFilterSelect,
                    onFilterClear = onFilterClear
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Game grid
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LotteryGameGrid(
                        games = uiState.filteredGames,
                        onGameClick = onGameClick
                    )
                }
            }
            
            // Right: Cart Panel (30%)
            LotteryCartPanel(
                cart = uiState.cart,
                totalDue = uiState.totalDue,
                isProcessing = uiState.isProcessing,
                canProcess = uiState.canProcessSale,
                onQuantityChange = onQuantityChange,
                onRemoveItem = onRemoveItem,
                onClearCart = onClearCart,
                onProcessSale = onProcessSale,
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun LotteryFilterChips(
    selectedFilter: LotteryGameType?,
    onFilterSelect: (LotteryGameType) -> Unit,
    onFilterClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = onFilterClear,
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("filter_all")
        )
        
        FilterChip(
            selected = selectedFilter == LotteryGameType.SCRATCHER,
            onClick = { onFilterSelect(LotteryGameType.SCRATCHER) },
            label = { Text("Scratchers") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFF6B35),
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("filter_scratchers")
        )
        
        FilterChip(
            selected = selectedFilter == LotteryGameType.DRAW,
            onClick = { onFilterSelect(LotteryGameType.DRAW) },
            label = { Text("Draw Games") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF2196F3),
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("filter_draw")
        )
    }
}

@Composable
fun LotteryGameGrid(
    games: List<LotteryGame>,
    onGameClick: (LotteryGame) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("game_grid")
    ) {
        items(games, key = { it.id }) { game ->
            LotteryGameCard(
                game = game,
                onClick = { onGameClick(game) }
            )
        }
    }
}

@Composable
fun LotteryGameCard(
    game: LotteryGame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (game.type) {
        LotteryGameType.SCRATCHER -> Color(0xFFFF6B35).copy(alpha = 0.15f)
        LotteryGameType.DRAW -> Color(0xFF2196F3).copy(alpha = 0.15f)
    }
    
    val accentColor = when (game.type) {
        LotteryGameType.SCRATCHER -> Color(0xFFFF6B35)
        LotteryGameType.DRAW -> Color(0xFF2196F3)
    }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(100.dp)
            .testTag("game_card_${game.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Game type badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (game.type == LotteryGameType.SCRATCHER) "Scratch" else "Draw",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            
            // Game name
            Text(
                text = game.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Price
            Text(
                text = currencyFormatter.format(game.ticketPrice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
fun LotteryCartPanel(
    cart: List<LotteryCartItem>,
    totalDue: BigDecimal,
    isProcessing: Boolean,
    canProcess: Boolean,
    onQuantityChange: (gameId: String, newQuantity: Int) -> Unit,
    onRemoveItem: (gameId: String) -> Unit,
    onClearCart: () -> Unit,
    onProcessSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (cart.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge {
                        Text(cart.sumOf { it.quantity }.toString())
                    }
                }
            }
            
            if (cart.isNotEmpty()) {
                IconButton(onClick = onClearCart) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear cart",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        
        // Cart items
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap games to add",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart, key = { it.gameId }) { item ->
                    LotteryCartItemRow(
                        item = item,
                        onQuantityChange = { qty -> onQuantityChange(item.gameId, qty) },
                        onRemove = { onRemoveItem(item.gameId) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        // Total
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Total Due",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currencyFormatter.format(totalDue),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Process button
        Button(
            onClick = onProcessSale,
            enabled = canProcess,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("process_sale_button")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Process Sale", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LotteryCartItemRow(
    item: LotteryCartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Game info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.gameName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${currencyFormatter.format(item.ticketPrice)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Quantity controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Increase",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Line total
            Text(
                text = currencyFormatter.format(item.lineTotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(64.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

