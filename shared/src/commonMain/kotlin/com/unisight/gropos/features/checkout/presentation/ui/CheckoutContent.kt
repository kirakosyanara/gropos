package com.unisight.gropos.features.checkout.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unisight.gropos.features.checkout.presentation.CheckoutItemUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutTotalsUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutUiState
import com.unisight.gropos.features.checkout.presentation.ScanEvent

// ============================================================================
// Test Tags (per testing-strategy.mdc: Use testTag for UI testing)
// ============================================================================
object CheckoutTestTags {
    const val SCREEN = "checkout_screen"
    const val TOP_APP_BAR = "checkout_top_app_bar"
    const val LOGOUT_BUTTON = "checkout_logout_button"
    const val ITEMS_LIST = "checkout_items_list"
    const val TOTALS_PANEL = "checkout_totals_panel"
    const val EMPTY_STATE = "checkout_empty_state"
    const val LOADING_INDICATOR = "checkout_loading"
    const val SCAN_FEEDBACK = "checkout_scan_feedback"
    const val GRAND_TOTAL = "checkout_grand_total"
    fun itemRow(branchProductId: Int) = "checkout_item_$branchProductId"
    fun snapBadge(branchProductId: Int) = "snap_badge_$branchProductId"
}

// ============================================================================
// Colors (POS-specific colors)
// ============================================================================
private val SnapGreen = Color(0xFF2E7D32)
private val SnapBadgeBackground = Color(0xFFE8F5E9)
private val SavingsColor = Color(0xFFD32F2F)

/**
 * Main content composable for the Checkout screen.
 * 
 * Standard POS layout:
 * - TopAppBar with title and logout action
 * - Left/Top: List of items (LazyColumn)
 * - Right/Bottom: Totals panel
 * 
 * Per code-quality.mdc: State hoisting - receives state and emits events.
 * Per Governance: No math here - all values are pre-formatted strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutContent(
    state: CheckoutUiState,
    onEvent: (CheckoutEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(CheckoutTestTags.SCREEN),
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag(CheckoutTestTags.TOP_APP_BAR),
                title = {
                    Text(
                        text = "GroPOS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Logout button
                    IconButton(
                        onClick = { onEvent(CheckoutEvent.Logout) },
                        modifier = Modifier.testTag(CheckoutTestTags.LOGOUT_BUTTON)
                    ) {
                        // Using text instead of icon for simplicity (no icon imports needed)
                        Text(
                            text = "⏻",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Left Panel: Items List
                ItemsListPanel(
                    items = state.items,
                    isEmpty = state.isEmpty,
                    onRemoveItem = { id -> onEvent(CheckoutEvent.RemoveItem(id)) },
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
                
                // Right Panel: Totals
                TotalsPanel(
                    totals = state.totals,
                    onClearCart = { onEvent(CheckoutEvent.ClearCart) },
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )
            }
            
            // Loading Overlay
            if (state.isLoading) {
                LoadingOverlay()
            }
            
            // Scan Feedback Snackbar
            state.lastScanEvent?.let { event ->
                ScanFeedbackSnackbar(
                    event = event,
                    onDismiss = { onEvent(CheckoutEvent.DismissScanEvent) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Left panel containing the list of scanned items.
 */
@Composable
private fun ItemsListPanel(
    items: List<CheckoutItemUiModel>,
    isEmpty: Boolean,
    onRemoveItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        if (isEmpty) {
            EmptyCartState(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(CheckoutTestTags.EMPTY_STATE)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag(CheckoutTestTags.ITEMS_LIST),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = items,
                    key = { it.branchProductId }
                ) { item ->
                    CartItemRow(
                        item = item,
                        onRemove = { onRemoveItem(item.branchProductId) },
                        modifier = Modifier.testTag(
                            CheckoutTestTags.itemRow(item.branchProductId)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Individual cart item row.
 */
@Composable
private fun CartItemRow(
    item: CheckoutItemUiModel,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantity
            Text(
                text = item.quantity,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp)
            )
            
            // Product Name + SNAP Badge
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // SNAP Badge (per requirement: show badge for SNAP eligible items)
                    if (item.isSnapEligible) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SnapBadge(
                            modifier = Modifier.testTag(
                                CheckoutTestTags.snapBadge(item.branchProductId)
                            )
                        )
                    }
                }
                
                // Savings if any
                if (item.hasSavings && item.savingsAmount != null) {
                    Text(
                        text = "Save ${item.savingsAmount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavingsColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Unit Price
            Text(
                text = item.unitPrice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Line Total
            Text(
                text = item.lineTotal,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
            )
            
            // Remove Button (simplified, no icon import needed)
            IconButton(onClick = onRemove) {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * SNAP eligibility badge.
 * 
 * Per requirement: Display SNAP badge for eligible items.
 */
@Composable
private fun SnapBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = SnapBadgeBackground
    ) {
        Text(
            text = "SNAP",
            style = MaterialTheme.typography.labelSmall,
            color = SnapGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Right panel showing totals.
 */
@Composable
private fun TotalsPanel(
    totals: CheckoutTotalsUiModel,
    onClearCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(CheckoutTestTags.TOTALS_PANEL),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Item Count Header
            Text(
                text = totals.itemCount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Totals Breakdown
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                TotalRow(label = "Subtotal", value = totals.subtotal)
                
                // Savings (if any)
                totals.savingsTotal?.let { savings ->
                    TotalRow(
                        label = "Savings",
                        value = savings,
                        valueColor = SavingsColor
                    )
                }
                
                TotalRow(label = "Tax", value = totals.taxTotal)
                
                // CRV (only show if non-zero)
                if (totals.crvTotal != "$0.00") {
                    TotalRow(label = "CRV", value = totals.crvTotal)
                }
                
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                
                // Grand Total (prominent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CheckoutTestTags.GRAND_TOTAL),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = totals.grandTotal,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onClearCart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
                
                // Pay button placeholder (for future payment integration)
                androidx.compose.material3.Button(
                    onClick = { /* TODO: Navigate to payment */ },
                    modifier = Modifier.weight(2f)
                ) {
                    Text("Pay")
                }
            }
        }
    }
}

/**
 * Individual row in the totals panel.
 */
@Composable
private fun TotalRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * Empty cart placeholder.
 */
@Composable
private fun EmptyCartState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛒",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cart is empty",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan an item to begin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Loading overlay during scan processing.
 */
@Composable
private fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f))
            .testTag(CheckoutTestTags.LOADING_INDICATOR),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Snackbar for scan feedback.
 */
@Composable
private fun ScanFeedbackSnackbar(
    event: ScanEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (message, isError) = when (event) {
        is ScanEvent.ProductAdded -> "Added: ${event.productName}" to false
        is ScanEvent.ProductNotFound -> "Product not found: ${event.barcode}" to true
        is ScanEvent.Error -> "Error: ${event.message}" to true
    }
    
    Snackbar(
        modifier = modifier
            .padding(16.dp)
            .testTag(CheckoutTestTags.SCAN_FEEDBACK),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
        containerColor = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    ) {
        Text(message)
    }
}

