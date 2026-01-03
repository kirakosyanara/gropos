package com.unisight.gropos.features.checkout.presentation.components.dialogs

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyMode
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.checkout.presentation.PriceCheckDialogState

/**
 * Price Check Dialog
 * 
 * Per FUNCTIONS_MENU.md (Price Check section):
 * - Scan or enter barcode to see price without adding to cart
 * - Shows product name, price, and SNAP eligibility
 * - No manager approval required
 */
@Composable
fun PriceCheckDialog(
    state: PriceCheckDialogState,
    onDigitPress: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onLookup: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.width(480.dp),
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
                    text = "Price Check",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryBlue
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Barcode Input Display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.White
                ) {
                    Text(
                        text = state.barcodeInput.ifEmpty { "Enter or scan barcode" },
                        modifier = Modifier.padding(GroPOSSpacing.M),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (state.barcodeInput.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                        color = if (state.barcodeInput.isEmpty()) GroPOSColors.TextSecondary else GroPOSColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Product Info Display
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GroPOSColors.PrimaryBlue)
                    }
                } else if (state.hasProduct) {
                    // Product found - show details
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(GroPOSRadius.Small),
                        color = GroPOSColors.SnapBadgeBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(GroPOSSpacing.M),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.productName ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                            Text(
                                text = state.productPrice ?: "",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.PrimaryGreen
                            )
                            if (state.isSnapEligible) {
                                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                                Surface(
                                    shape = RoundedCornerShape(GroPOSRadius.Small),
                                    color = GroPOSColors.SnapGreen
                                ) {
                                    Text(
                                        text = "SNAP Eligible",
                                        modifier = Modifier.padding(
                                            horizontal = GroPOSSpacing.M,
                                            vertical = GroPOSSpacing.XS
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GroPOSColors.White
                                    )
                                }
                            }
                        }
                    }
                } else if (state.errorMessage != null) {
                    // Error - product not found
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(GroPOSRadius.Small),
                        color = Color(0xFFFFEBEE) // Light red
                    ) {
                        Column(
                            modifier = Modifier.padding(GroPOSSpacing.M),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Not Found",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.DangerRed
                            )
                            Spacer(modifier = Modifier.height(GroPOSSpacing.XS))
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GroPOSColors.TextPrimary
                            )
                        }
                    }
                } else {
                    // Empty state - instructions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Scan barcode or enter manually",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // TenKey
                TenKey(
                    state = TenKeyState(
                        inputValue = state.barcodeInput,
                        mode = TenKeyMode.Digit
                    ),
                    onDigitClick = onDigitPress,
                    onClearClick = onClear,
                    onBackspaceClick = onBackspace,
                    onOkClick = { onLookup() },
                    showQtyButton = false,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                ) {
                    OutlineButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    
                    PrimaryButton(
                        onClick = onLookup,
                        enabled = state.barcodeInput.isNotEmpty() && !state.isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lookup")
                    }
                }
            }
        }
    }
}
