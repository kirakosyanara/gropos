package com.unisight.gropos.features.customer.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.checkout.presentation.CheckoutItemUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutTotalsUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutUiState

/**
 * Customer Display Screen (Voyager Screen)
 * 
 * Per SCREEN_LAYOUTS.md (Customer Screen):
 * - Secondary display for customer viewing
 * - Shows order items, totals, and advertisements
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Observes CartRepository via CustomerDisplayViewModel
 * - Read-only view of cart state
 * - State synchronized with Cashier window automatically
 * 
 * Layout:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ [Store Name]     [Savings: $X.XX]     [Weight: 0.0 lb]         │
 * ├────────────────────────────────────────┬────────────────────────┤
 * │         ORDER ITEMS                    │      TOTALS            │
 * │  [Product 1]         $XX.XX            │  Subtotal     $XX.XX   │
 * │  [Product 2]         $XX.XX            │  Sales Tax     $X.XX   │
 * │  [Product 3]         $XX.XX            │  Items: X  Total $X.XX │
 * │  ...                                   │                        │
 * │                                        │  ┌── SNAP ───────────┐ │
 * │                                        │  │ SNAP Eligible $X  │ │
 * │                                        │  └───────────────────┘ │
 * │                                        │                        │
 * │                                        │  ┌── Advertisement ──┐ │
 * │                                        │  │   [Ad Image]      │ │
 * │                                        │  └───────────────────┘ │
 * └────────────────────────────────────────┴────────────────────────┘
 */
class CustomerDisplayVoyagerScreen(
    private val storeName: String = "GroPOS Store"
) : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CustomerDisplayViewModel>()
        val state by viewModel.state.collectAsState()
        
        CustomerDisplayContent(
            items = state.items,
            totals = state.totals,
            storeName = storeName,
            isEmpty = state.isEmpty
        )
    }
}

/**
 * Composable wrapper for use in desktop Window (non-Voyager context).
 * 
 * This is used in Main.kt for the secondary customer display window.
 * It creates its own ViewModel via Koin.
 */
@Composable
fun CustomerDisplayScreen(
    viewModel: CustomerDisplayViewModel,
    storeName: String = "GroPOS Store"
) {
    val state by viewModel.state.collectAsState()
    
    CustomerDisplayContent(
        items = state.items,
        totals = state.totals,
        storeName = storeName,
        isEmpty = state.isEmpty
    )
}

@Composable
fun CustomerDisplayContent(
    items: List<CheckoutItemUiModel>,
    totals: CheckoutTotalsUiModel,
    storeName: String,
    isEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GroPOSColors.PrimaryGreen)
    ) {
        // Header Bar
        CustomerHeader(
            storeName = storeName,
            savingsTotal = totals.savingsTotal
        )
        
        // Main Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(GroPOSSpacing.M)
        ) {
            // Left Side: Order Items (60%)
            OrderItemsPanel(
                items = items,
                isEmpty = isEmpty,
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            )
            
            Spacer(modifier = Modifier.width(GroPOSSpacing.M))
            
            // Right Side: Totals + Advertisement (40%)
            TotalsAndAdPanel(
                totals = totals,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            )
        }
    }
}

// ============================================================================
// Header Component
// ============================================================================

@Composable
private fun CustomerHeader(
    storeName: String,
    savingsTotal: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GroPOSColors.PrimaryGreenHover
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GroPOSSpacing.XL, vertical = GroPOSSpacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store Name
            Text(
                text = storeName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.White
            )
            
            // Savings (if any)
            savingsTotal?.let { savings ->
                Surface(
                    shape = RoundedCornerShape(GroPOSRadius.Pill),
                    color = GroPOSColors.WarningOrange
                ) {
                    Text(
                        text = "You Save: $savings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.White,
                        modifier = Modifier.padding(horizontal = GroPOSSpacing.L, vertical = GroPOSSpacing.S)
                    )
                }
            }
            
            // Weight placeholder
            Text(
                text = "Weight: 0.00 lb",
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.White.copy(alpha = 0.8f)
            )
        }
    }
}

// ============================================================================
// Order Items Panel
// ============================================================================

@Composable
private fun OrderItemsPanel(
    items: List<CheckoutItemUiModel>,
    isEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    WhiteBox(modifier = modifier) {
        if (isEmpty) {
            // Welcome / Idle State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome!",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    Text(
                        text = "Please wait while we ring up your items",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GroPOSColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Order Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
            ) {
                items(
                    items = items,
                    key = { it.branchProductId }
                ) { item ->
                    CustomerOrderItem(item = item)
                    HorizontalDivider(color = GroPOSColors.LightGray3)
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderItem(
    item: CheckoutItemUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = GroPOSSpacing.S),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quantity + Name
        Row(
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Quantity badge
            Surface(
                shape = RoundedCornerShape(GroPOSRadius.Small),
                color = GroPOSColors.PrimaryGreen
            ) {
                Text(
                    text = item.quantity,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.White,
                    modifier = Modifier.padding(horizontal = GroPOSSpacing.S, vertical = 4.dp)
                )
            }
            
            // Product name
            Column {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = GroPOSColors.TextPrimary
                )
                
                // SNAP indicator
                if (item.isSnapEligible) {
                    Text(
                        text = "SNAP Eligible",
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.SnapGreen
                    )
                }
            }
        }
        
        // Price
        Text(
            text = item.lineTotal,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.PrimaryGreen
        )
    }
}

// ============================================================================
// Totals and Advertisement Panel
// ============================================================================

@Composable
private fun TotalsAndAdPanel(
    totals: CheckoutTotalsUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
    ) {
        // Totals Card
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
            ) {
                // Item Count
                Text(
                    text = totals.itemCount,
                    style = MaterialTheme.typography.bodyLarge,
                    color = GroPOSColors.TextSecondary
                )
                
                HorizontalDivider(color = GroPOSColors.LightGray3)
                
                // Subtotal
                TotalRow(label = "Subtotal", value = totals.subtotal)
                
                // Tax
                TotalRow(label = "Tax", value = totals.taxTotal)
                
                // CRV if applicable
                if (totals.crvTotal != "$0.00") {
                    TotalRow(label = "CRV", value = totals.crvTotal)
                }
                
                HorizontalDivider(color = GroPOSColors.LightGray3)
                
                // Grand Total (Large)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = totals.grandTotal,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.PrimaryGreen
                    )
                }
            }
        }
        
        // SNAP Eligible Box (if applicable)
        // TODO: Add SNAP eligible amount from state
        
        // Advertisement Placeholder
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏷️",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                    Text(
                        text = "Advertisement Space",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GroPOSColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
