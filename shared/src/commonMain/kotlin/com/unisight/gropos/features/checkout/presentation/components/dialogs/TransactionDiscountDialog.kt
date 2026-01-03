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
import androidx.compose.ui.text.style.TextDecoration
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
import com.unisight.gropos.features.checkout.presentation.TransactionDiscountDialogState

/**
 * Transaction Discount Dialog
 * 
 * Per FUNCTIONS_MENU.md (Discount section):
 * - Apply percentage discount to entire order
 * - Requires Manager approval
 * - Shows before/after totals
 */
@Composable
fun TransactionDiscountDialog(
    state: TransactionDiscountDialogState,
    onDigitPress: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    
    // Format percentage display
    val percentDisplay = if (state.inputValue.isNotEmpty()) {
        "${state.inputValue}%"
    } else {
        "0%"
    }
    
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
                    text = "Transaction Discount",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.SavingsRed
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Totals Preview
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.White
                ) {
                    Column(
                        modifier = Modifier.padding(GroPOSSpacing.M)
                    ) {
                        // Current Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Total:",
                                style = MaterialTheme.typography.titleMedium,
                                color = GroPOSColors.TextSecondary
                            )
                            Text(
                                text = state.currentTotal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.TextPrimary,
                                textDecoration = if (state.discountedTotal != null) TextDecoration.LineThrough else null
                            )
                        }
                        
                        // Discounted Total (if calculated)
                        if (state.discountedTotal != null) {
                            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "After Discount:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GroPOSColors.PrimaryGreen
                                )
                                Text(
                                    text = state.discountedTotal,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GroPOSColors.PrimaryGreen
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Discount Percentage Input
                Text(
                    text = "Discount Percentage",
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
                        text = percentDisplay,
                        modifier = Modifier.padding(GroPOSSpacing.M),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.SavingsRed,
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
                            inputValue = percentDisplay,
                            mode = TenKeyMode.Discount
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
                        CircularProgressIndicator(color = GroPOSColors.SavingsRed)
                        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                        Text(
                            text = if (state.approvalPending) "Awaiting Manager Approval..." else "Applying discount...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Manager Approval Notice
                if (!state.isProcessing) {
                    Text(
                        text = "Manager approval required",
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.WarningOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                }
                
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
                        enabled = state.inputValue.isNotEmpty() && !state.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Discount")
                    }
                }
            }
        }
    }
}

