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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.PrimaryButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Hold Transaction Dialog.
 * 
 * Per TRANSACTION_FLOW.md: Simple popup asking for an optional "Name/Note".
 * Per SCREEN_LAYOUTS.md: Hold Dialog structure with Recall Name field.
 * 
 * @param grandTotal The current transaction total (for display)
 * @param itemCount The number of items in the cart
 * @param onHold Called when user clicks "Hold" with optional note
 * @param onDismiss Called when user cancels
 */
@Composable
fun HoldTransactionDialog(
    grandTotal: String,
    itemCount: String,
    onHold: (holdName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var holdName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .padding(GroPOSSpacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Hold Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Transaction summary
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Items: $itemCount",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GroPOSColors.TextSecondary
                    )
                    Text(
                        text = "Total: $grandTotal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GroPOSColors.TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Hold name input
                OutlinedTextField(
                    value = holdName,
                    onValueChange = { holdName = it },
                    label = { Text("Recall Name (Optional)") },
                    placeholder = { Text("e.g., Guy in blue shirt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                
                Text(
                    text = "Enter a name to help identify this transaction later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                ) {
                    OutlineButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    PrimaryButton(
                        onClick = { 
                            onHold(holdName.takeIf { it.isNotBlank() })
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hold")
                    }
                }
            }
        }
    }
}

