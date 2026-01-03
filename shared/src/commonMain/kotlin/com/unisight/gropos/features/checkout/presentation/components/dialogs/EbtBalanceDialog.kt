package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.PrimaryButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.checkout.presentation.EbtBalanceDialogState

/**
 * EBT Balance Check Dialog
 * 
 * Per FUNCTIONS_MENU.md (EBT Balance section):
 * - Check customer's EBT balance before payment
 * - Requires card swipe/insertion
 * - Shows food stamp and cash balances
 */
@Composable
fun EbtBalanceDialog(
    state: EbtBalanceDialogState,
    onInquiry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    
    Dialog(onDismissRequest = { if (!state.isProcessing) onDismiss() }) {
        Card(
            modifier = modifier.width(400.dp),
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
                // Title with icon
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "EBT Card",
                    modifier = Modifier.size(48.dp),
                    tint = GroPOSColors.PrimaryBlue
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                
                Text(
                    text = "EBT Balance Check",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryBlue
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Content based on state
                if (state.isProcessing) {
                    // Processing state
                    Column(
                        modifier = Modifier.height(150.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = GroPOSColors.PrimaryBlue)
                        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                        Text(
                            text = "Waiting for card...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GroPOSColors.TextSecondary
                        )
                        Text(
                            text = "Please insert or swipe EBT card",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                } else if (state.hasResult) {
                    // Balance result
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(GroPOSRadius.Small),
                        color = GroPOSColors.SnapBadgeBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(GroPOSSpacing.M)
                        ) {
                            // Food Stamp Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Food Stamp (SNAP):",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GroPOSColors.TextPrimary
                                )
                                Text(
                                    text = state.foodStampBalance ?: "$0.00",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GroPOSColors.SnapGreen
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                            
                            // Cash Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "EBT Cash:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GroPOSColors.TextPrimary
                                )
                                Text(
                                    text = state.cashBalance ?: "$0.00",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GroPOSColors.PrimaryBlue
                                )
                            }
                        }
                    }
                } else if (state.errorMessage != null) {
                    // Error state
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(GroPOSRadius.Small),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Column(
                            modifier = Modifier.padding(GroPOSSpacing.M),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Balance Inquiry Failed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.DangerRed
                            )
                            Spacer(modifier = Modifier.height(GroPOSSpacing.XS))
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GroPOSColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Initial state - instructions
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(GroPOSRadius.Small),
                        color = GroPOSColors.White
                    ) {
                        Column(
                            modifier = Modifier.padding(GroPOSSpacing.M),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Press 'Check Balance' and have the customer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GroPOSColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "insert or swipe their EBT card",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GroPOSColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
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
                        Text(if (state.hasResult) "Close" else "Cancel")
                    }
                    
                    if (!state.hasResult) {
                        PrimaryButton(
                            onClick = onInquiry,
                            enabled = !state.isProcessing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Check Balance")
                        }
                    }
                }
            }
        }
    }
}

