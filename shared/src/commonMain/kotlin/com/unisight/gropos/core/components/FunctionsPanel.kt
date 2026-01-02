package com.unisight.gropos.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * GroPOS Functions Panel
 * 
 * Per FUNCTIONS_MENU.md: Tabbed panel for accessing POS functions.
 * Organized into three tabs: Recall, Payments, Till
 */

// ============================================================================
// Function Actions (per FUNCTIONS_MENU.md)
// ============================================================================

/**
 * Available function actions in the POS system.
 */
enum class FunctionAction {
    // Recall Tab
    PRINT_LAST_RECEIPT,
    REPORT,
    VOID_TRANSACTION,
    PULL_BACK,
    RUN_TEST,
    
    // Payments Tab
    VENDOR_PAYOUT,
    CASH_PICKUP,
    LOTTO_PAY,
    TRANSACTION_DISCOUNT,
    
    // Till Tab
    OPEN_DRAWER,
    PRICE_CHECK,
    ADD_CASH,
    BALANCE_CHECK,
    
    // System
    SIGN_OUT,
    BACK
}

// ============================================================================
// Functions Grid (Simplified for Walking Skeleton)
// ============================================================================

/**
 * Functions Grid - Quick action buttons displayed on checkout screen.
 * 
 * Per SCREEN_LAYOUTS.md (Home Screen):
 * - Actions panel at bottom of right section
 * - Includes: [Lookup][Recall] and [Functions]
 * 
 * Per FUNCTIONS_MENU.md: Void Transaction is a key action
 */
@Composable
fun FunctionsGrid(
    onFunctionsClick: () -> Unit,
    onLookupClick: () -> Unit,
    onRecallClick: () -> Unit,
    onVoidTransactionClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(GroPOSSpacing.S),
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        // Row 1: Lookup and Recall
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            PrimaryButton(
                onClick = onLookupClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Lookup")
            }
            OutlineButton(
                onClick = onRecallClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Recall")
            }
        }
        
        // Row 2: Void and Functions
        // Per FUNCTIONS_MENU.md: Void Transaction is frequently used
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            DangerButton(
                onClick = onVoidTransactionClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Void")
            }
            OutlineButton(
                onClick = onFunctionsClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Functions")
            }
        }
        
        // Row 3: Sign Out
        // Per CASHIER_OPERATIONS.md: Sign Out shows logout options dialog
        DangerButton(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}

// ============================================================================
// Full Functions Panel (Modal/Overlay)
// ============================================================================

/**
 * Full Functions Panel with tabbed sections.
 * 
 * Per FUNCTIONS_MENU.md:
 * - Three tabs: Recall, Payments, Till
 * - Sign Out button at bottom (Danger style)
 * - Back button to dismiss
 */
@Composable
fun FunctionsPanel(
    onActionClick: (FunctionAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Recall", "Payments", "Till")
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = GroPOSColors.White
    ) {
        Column(modifier = Modifier.padding(GroPOSSpacing.XL)) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = GroPOSColors.LightGray1
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge
                            ) 
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(GroPOSSpacing.M))
            
            // Tab Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
            ) {
                when (selectedTab) {
                    0 -> RecallTabContent(onActionClick)
                    1 -> PaymentsTabContent(onActionClick)
                    2 -> TillTabContent(onActionClick)
                }
            }
            
            Spacer(Modifier.height(GroPOSSpacing.M))
            
            // Footer Buttons
            Column(verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)) {
                DangerButton(
                    onClick = { onActionClick(FunctionAction.SIGN_OUT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out")
                }
                OutlineButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}

// ============================================================================
// Tab Content Composables
// ============================================================================

@Composable
private fun RecallTabContent(onActionClick: (FunctionAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)) {
        FunctionButton(
            text = "Return/Invoice",
            onClick = { onActionClick(FunctionAction.REPORT) }
        )
        FunctionButton(
            text = "Pullback",
            onClick = { onActionClick(FunctionAction.PULL_BACK) }
        )
        FunctionButton(
            text = "Void Transaction",
            onClick = { onActionClick(FunctionAction.VOID_TRANSACTION) }
        )
        FunctionButton(
            text = "Print Last Receipt",
            onClick = { onActionClick(FunctionAction.PRINT_LAST_RECEIPT) }
        )
        FunctionButton(
            text = "Run Test",
            onClick = { onActionClick(FunctionAction.RUN_TEST) }
        )
    }
}

@Composable
private fun PaymentsTabContent(onActionClick: (FunctionAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)) {
        FunctionButton(
            text = "Vendor Payout",
            onClick = { onActionClick(FunctionAction.VENDOR_PAYOUT) }
        )
        FunctionButton(
            text = "Cash Pickup",
            onClick = { onActionClick(FunctionAction.CASH_PICKUP) }
        )
        FunctionButton(
            text = "Lotto Pay",
            onClick = { onActionClick(FunctionAction.LOTTO_PAY) }
        )
        FunctionButton(
            text = "Discount",
            onClick = { onActionClick(FunctionAction.TRANSACTION_DISCOUNT) }
        )
    }
}

@Composable
private fun TillTabContent(onActionClick: (FunctionAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)) {
        FunctionButton(
            text = "Open Drawer",
            onClick = { onActionClick(FunctionAction.OPEN_DRAWER) }
        )
        FunctionButton(
            text = "Price Check",
            onClick = { onActionClick(FunctionAction.PRICE_CHECK) }
        )
        FunctionButton(
            text = "Add Cash",
            onClick = { onActionClick(FunctionAction.ADD_CASH) }
        )
        FunctionButton(
            text = "EBT Balance",
            onClick = { onActionClick(FunctionAction.BALANCE_CHECK) }
        )
    }
}

@Composable
private fun FunctionButton(
    text: String,
    onClick: () -> Unit
) {
    OutlineButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

