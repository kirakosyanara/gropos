package com.unisight.gropos.features.payment.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.components.ExtraLargeButton
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.PrimaryButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.components.WarningButton
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.payment.presentation.AppliedPaymentUiModel
import com.unisight.gropos.features.payment.presentation.PaymentSummaryUiModel
import com.unisight.gropos.features.payment.presentation.PaymentTab
import com.unisight.gropos.features.payment.presentation.PaymentUiState
import java.math.BigDecimal

/**
 * Payment Screen Content.
 * 
 * Per SCREEN_LAYOUTS.md (Payment Screen):
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ [Return to Items]                        [Hold Transaction]     │
 * ├──────────────────────────────────────────┬──────────────────────┤
 * │         LEFT SECTION                      │    RIGHT SECTION     │
 * │  ┌── Summary ───────────────────────┐     │ [CHARGE][EBT][CASH]  │
 * │  │  Subtotal, Tax, Total            │     │ [OTHER]              │
 * │  └──────────────────────────────────┘     │                      │
 * │  ┌── SNAP Eligible ─────────────────┐     │ Tab content...       │
 * │  │  Food Stamp Eligible   $XX.XX    │     │                      │
 * │  └──────────────────────────────────┘     │ ┌── Ten-Key ──────┐  │
 * │  ┌── Payments Applied ──────────────┐     │ │ [7] [8] [9]     │  │
 * │  │  [Payment list]                  │     │ │ [4] [5] [6]     │  │
 * │  │  Remaining: $XX.XX               │     │ │ [1] [2] [3]     │  │
 * │  └──────────────────────────────────┘     │ │ [.] [0] [OK]    │  │
 * │                                           │ └─────────────────┘  │
 * └───────────────────────────────────────────┴──────────────────────┘
 */
@Composable
fun PayContent(
    state: PaymentUiState,
    onEvent: (PaymentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GroPOSColors.LightGray1)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            PaymentHeader(
                onReturnToItems = { onEvent(PaymentEvent.ReturnToItems) },
                onHoldTransaction = { onEvent(PaymentEvent.HoldTransaction) }
            )
            
            // Main Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(GroPOSSpacing.M)
            ) {
                // Left Section (60%)
                LeftSection(
                    summary = state.summary,
                    appliedPayments = state.appliedPayments,
                    remainingAmount = state.remainingAmount,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
                
                Spacer(modifier = Modifier.width(GroPOSSpacing.M))
                
                // Right Section (40%)
                RightSection(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )
            }
        }
        
        // Processing Overlay
        if (state.isProcessing) {
            ProcessingOverlay()
        }
        
        // Change Dialog
        if (state.showChangeDialog) {
            ChangeDialog(
                changeAmount = state.changeAmount,
                onDismiss = { onEvent(PaymentEvent.DismissChangeDialog) }
            )
        }
        
        // Error Dialog
        state.errorMessage?.let { message ->
            ErrorDialog(
                message = message,
                onDismiss = { onEvent(PaymentEvent.DismissError) }
            )
        }
    }
}

// ============================================================================
// Header
// ============================================================================

@Composable
private fun PaymentHeader(
    onReturnToItems: () -> Unit,
    onHoldTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GroPOSColors.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlineButton(onClick = onReturnToItems) {
                Text("← Return to Items")
            }
            
            Text(
                text = "Payment",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            WarningButton(onClick = onHoldTransaction) {
                Text("Hold Transaction")
            }
        }
    }
}

// ============================================================================
// Left Section - Summary and Payments
// ============================================================================

@Composable
private fun LeftSection(
    summary: PaymentSummaryUiModel,
    appliedPayments: List<AppliedPaymentUiModel>,
    remainingAmount: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
    ) {
        // Summary Card
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            SummaryContent(summary = summary)
        }
        
        // SNAP Eligible Card
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            SnapEligibleContent(
                snapEligibleTotal = summary.snapEligibleTotal,
                nonSnapTotal = summary.nonSnapTotal
            )
        }
        
        // Payments Applied Card
        WhiteBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PaymentsAppliedContent(
                payments = appliedPayments,
                remainingAmount = remainingAmount
            )
        }
    }
}

@Composable
private fun SummaryContent(
    summary: PaymentSummaryUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.XS)
    ) {
        Text(
            text = "Transaction Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        HorizontalDivider(color = GroPOSColors.LightGray3)
        
        SummaryRow(label = "Item Subtotal", value = summary.itemSubtotal)
        
        summary.discountTotal?.let { discount ->
            SummaryRow(
                label = "Discount",
                value = discount,
                valueColor = GroPOSColors.DangerRed
            )
        }
        
        HorizontalDivider(color = GroPOSColors.LightGray3)
        
        SummaryRow(label = "Subtotal", value = summary.subtotal)
        
        summary.crvTotal?.let { crv ->
            SummaryRow(label = "Bottle Fee", value = crv)
        }
        
        SummaryRow(label = "Tax", value = summary.taxTotal)
        
        HorizontalDivider(color = GroPOSColors.LightGray3)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary.itemCount,
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.TextSecondary
            )
            
            Text(
                text = summary.grandTotal,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.PrimaryGreen
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = GroPOSColors.TextPrimary,
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
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun SnapEligibleContent(
    snapEligibleTotal: String,
    nonSnapTotal: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            Surface(
                shape = RoundedCornerShape(GroPOSRadius.Small),
                color = GroPOSColors.SnapGreen.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "SNAP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.SnapGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(
                text = "Eligible Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SNAP Eligible",
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.SnapGreen
            )
            Text(
                text = snapEligibleTotal,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.SnapGreen
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Non-SNAP",
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.TextSecondary
            )
            Text(
                text = nonSnapTotal,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PaymentsAppliedContent(
    payments: List<AppliedPaymentUiModel>,
    remainingAmount: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Payments Applied",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        HorizontalDivider(
            color = GroPOSColors.LightGray3,
            modifier = Modifier.padding(vertical = GroPOSSpacing.S)
        )
        
        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No payments yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GroPOSColors.TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
            ) {
                items(payments, key = { it.id }) { payment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = payment.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            payment.details?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GroPOSColors.TextSecondary
                                )
                            }
                        }
                        Text(
                            text = payment.amount,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = GroPOSColors.PrimaryGreen
                        )
                    }
                }
            }
        }
        
        HorizontalDivider(
            color = GroPOSColors.LightGray3,
            modifier = Modifier.padding(vertical = GroPOSSpacing.S)
        )
        
        // Remaining
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Remaining",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = remainingAmount,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.DangerRed
            )
        }
    }
}

// ============================================================================
// Right Section - Payment Methods and TenKey
// ============================================================================

@Composable
private fun RightSection(
    state: PaymentUiState,
    onEvent: (PaymentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
    ) {
        // Payment Method Tabs
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            PaymentMethodTabs(
                selectedTab = state.selectedTab,
                onTabSelect = { onEvent(PaymentEvent.SelectTab(it)) },
                onEvent = onEvent
            )
        }
        
        // TenKey with entered amount
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)) {
                // Amount display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.LightGray1
                ) {
                    Text(
                        text = if (state.enteredAmount.isEmpty()) "0" else state.enteredAmount,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GroPOSSpacing.M)
                    )
                }
                
                // TenKey
                TenKey(
                    state = TenKeyState(inputValue = state.enteredAmount),
                    onDigitClick = { onEvent(PaymentEvent.DigitPress(it)) },
                    onClearClick = { onEvent(PaymentEvent.ClearPress) },
                    onBackspaceClick = { onEvent(PaymentEvent.BackspacePress) },
                    onOkClick = { 
                        when (state.selectedTab) {
                            PaymentTab.Cash -> onEvent(PaymentEvent.CashEnteredAmount)
                            else -> { /* Use entered amount for other types */ }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodTabs(
    selectedTab: PaymentTab,
    onTabSelect: (PaymentTab) -> Unit,
    onEvent: (PaymentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        PaymentTab.Cash to "CASH",
        PaymentTab.Charge to "CHARGE",
        PaymentTab.Ebt to "EBT",
        PaymentTab.Other to "OTHER"
    )
    
    Column(modifier = modifier) {
        TabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
            containerColor = GroPOSColors.White,
            contentColor = GroPOSColors.PrimaryGreen
        ) {
            tabs.forEach { (tab, label) ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelect(tab) },
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Tab content
        when (selectedTab) {
            PaymentTab.Cash -> CashTabContent(onEvent = onEvent)
            PaymentTab.Charge -> ChargeTabContent(onEvent = onEvent)
            PaymentTab.Ebt -> EbtTabContent(onEvent = onEvent)
            PaymentTab.Other -> OtherTabContent()
        }
    }
}

@Composable
private fun CashTabContent(
    onEvent: (PaymentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        // Quick cash buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            QuickCashButton("$1", BigDecimal("1.00"), onEvent, Modifier.weight(1f))
            QuickCashButton("$5", BigDecimal("5.00"), onEvent, Modifier.weight(1f))
            QuickCashButton("$10", BigDecimal("10.00"), onEvent, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            QuickCashButton("$20", BigDecimal("20.00"), onEvent, Modifier.weight(1f))
            QuickCashButton("$50", BigDecimal("50.00"), onEvent, Modifier.weight(1f))
            QuickCashButton("$100", BigDecimal("100.00"), onEvent, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        // Exact change button
        SuccessButton(
            onClick = { onEvent(PaymentEvent.CashExactChange) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exact Change", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickCashButton(
    label: String,
    amount: BigDecimal,
    onEvent: (PaymentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryButton(
        onClick = { onEvent(PaymentEvent.CashQuickAmount(amount)) },
        modifier = modifier.height(48.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChargeTabContent(
    onEvent: (PaymentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        PrimaryButton(
            onClick = { onEvent(PaymentEvent.CreditPayment) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Credit Card", fontWeight = FontWeight.Bold)
        }
        
        PrimaryButton(
            onClick = { onEvent(PaymentEvent.DebitPayment) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Debit Card", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EbtTabContent(
    onEvent: (PaymentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        SuccessButton(
            onClick = { onEvent(PaymentEvent.EbtSnapPayment) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SNAP (Food Stamp)", fontWeight = FontWeight.Bold)
        }
        
        PrimaryButton(
            onClick = { onEvent(PaymentEvent.EbtCashPayment) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("EBT Cash", fontWeight = FontWeight.Bold)
        }
        
        OutlineButton(
            onClick = { onEvent(PaymentEvent.BalanceCheck) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Balance Check")
        }
    }
}

@Composable
private fun OtherTabContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        OutlineButton(
            onClick = { /* TODO: Check payment */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check")
        }
        
        OutlineButton(
            onClick = { /* TODO: On Account */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("On Account")
        }
    }
}

// ============================================================================
// Dialogs
// ============================================================================

@Composable
private fun ProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
            ) {
                CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
                Text(
                    text = "Processing Payment...",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
private fun ChangeDialog(
    changeAmount: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.L)
            ) {
                Text(
                    text = "💵",
                    style = MaterialTheme.typography.displayLarge
                )
                
                Text(
                    text = "Change Due",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = changeAmount,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryGreen
                )
                
                SuccessButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.L)
            ) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displayLarge
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                
                PrimaryButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
        }
    }
}

