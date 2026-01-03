package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyMode
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.checkout.presentation.AddCashDialogState

/**
 * Add Cash Dialog
 * 
 * Per FUNCTIONS_MENU.md (Add Cash section):
 * - Add cash to the till drawer (e.g., starting float)
 * - Requires Manager approval for amounts over $100
 * - Updates drawer balance
 */
@Composable
fun AddCashDialog(
    state: AddCashDialogState,
    onDigitPress: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    
    // Format input as currency
    val amountCents = state.inputValue.toLongOrNull() ?: 0
    val formattedAmount = "$${amountCents / 100}.${(amountCents % 100).toString().padStart(2, '0')}"
    
    Dialog(onDismissRequest = { if (!state.isProcessing) onDismiss() }) {
        Card(
            modifier = modifier.width(420.dp),
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            colors = CardDefaults.cardColors(
                containerColor = GroPOSColors.LightGray1
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.L),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Add Cash to Drawer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryGreen
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Current Drawer Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Current Balance:",
                        style = MaterialTheme.typography.titleMedium,
                        color = GroPOSColors.TextSecondary
                    )
                    Text(
                        text = state.currentBalance,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Amount Input Display
                Text(
                    text = "Amount to Add",
                    style = MaterialTheme.typography.titleSmall,
                    color = GroPOSColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.XS))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.White
                ) {
                    Text(
                        text = formattedAmount,
                        modifier = Modifier.padding(GroPOSSpacing.M),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.PrimaryGreen,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Error message
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // TenKey or Processing state
                if (!state.isProcessing) {
                    TenKey(
                        state = TenKeyState(
                            inputValue = formattedAmount,
                            mode = TenKeyMode.CashPickup
                        ),
                        onDigitClick = onDigitPress,
                        onClearClick = onClear,
                        onBackspaceClick = onBackspace,
                        onOkClick = { onConfirm() },
                        showQtyButton = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Processing state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
                        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                        Text(
                            text = if (state.approvalPending) "Awaiting Manager Approval..." else "Processing...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                ) {
                    OutlineButton(
                        onClick = onDismiss,
                        enabled = !state.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    SuccessButton(
                        onClick = onConfirm,
                        enabled = amountCents > 0 && !state.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Cash")
                    }
                }
            }
        }
    }
}
