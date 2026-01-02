package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Cash Pickup Dialog for removing cash from the drawer for safe deposit.
 * 
 * Per FUNCTIONS_MENU.md:
 * - Cash Pickup removes cash from drawer for safe deposit
 * - Prerequisites: No active payments in current transaction, Manager approval required
 * - Flow: Enter pickup amount → Manager approval → Cash removed → Receipt printed
 * 
 * Per DIALOGS.md: Add Cash Dialog pattern
 * - Title bar with close button
 * - Amount input with TenKey
 * - Helper text showing current drawer balance
 * - Cancel and action buttons
 * 
 * @param currentBalance Current drawer balance (formatted string)
 * @param inputValue Current input value for the amount
 * @param errorMessage Optional error message to display
 * @param onDigitClick Called when a digit is pressed
 * @param onClearClick Called when CLR is pressed
 * @param onBackspaceClick Called when backspace is pressed
 * @param onPickupClick Called when Pickup button is pressed with the input amount
 * @param onDismiss Called when dialog should be dismissed
 */
@Composable
fun CashPickupDialog(
    currentBalance: String,
    inputValue: String,
    errorMessage: String? = null,
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onPickupClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(450.dp)
                .testTag("cash_pickup_dialog")
        ) {
            Column {
                // Header
                CashPickupHeader(onClose = onDismiss)
                
                // Content
                Column(
                    modifier = Modifier
                        .padding(GroPOSSpacing.M)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Helper text: Current Drawer Balance
                    Text(
                        text = "Current Drawer Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.TextSecondary
                    )
                    
                    Text(
                        text = currentBalance,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.PrimaryGreen,
                        modifier = Modifier.testTag("drawer_balance")
                    )
                    
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    
                    // Amount input display
                    Text(
                        text = "Enter pickup amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(GroPOSSpacing.XS))
                    
                    // Amount display box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = GroPOSColors.LightGray2
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatAmountDisplay(inputValue),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("pickup_amount_display")
                            )
                        }
                    }
                    
                    // Error message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(GroPOSSpacing.XS))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = GroPOSColors.DangerRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("error_message")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    
                    // TenKey pad
                    TenKey(
                        state = TenKeyState(inputValue = inputValue),
                        onDigitClick = onDigitClick,
                        onOkClick = { onPickupClick() },
                        onClearClick = onClearClick,
                        onBackspaceClick = onBackspaceClick,
                        showQtyButton = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                    ) {
                        OutlineButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cancel_button")
                        ) {
                            Text("Cancel")
                        }
                        
                        DangerButton(
                            onClick = onPickupClick,
                            enabled = inputValue.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pickup_button")
                        ) {
                            Text("Pickup")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header for the Cash Pickup dialog.
 */
@Composable
private fun CashPickupHeader(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GroPOSColors.PrimaryGreen)
            .padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.S),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cash Pickup",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("close_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

/**
 * Formats the input value as a currency amount.
 */
private fun formatAmountDisplay(input: String): String {
    if (input.isBlank()) return "$0.00"
    
    // If input contains a decimal, format accordingly
    return if (input.contains(".")) {
        val parts = input.split(".")
        val dollars = parts[0].ifEmpty { "0" }
        val cents = parts.getOrElse(1) { "00" }.take(2).padEnd(2, '0')
        "$$dollars.$cents"
    } else {
        // Treat as cents (e.g., "500" = "$5.00")
        val value = input.toLongOrNull() ?: 0
        val dollars = value / 100
        val cents = value % 100
        String.format("$%d.%02d", dollars, cents)
    }
}

