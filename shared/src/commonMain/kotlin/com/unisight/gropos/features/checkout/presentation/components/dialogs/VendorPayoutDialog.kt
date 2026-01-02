package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.unisight.gropos.features.cashier.domain.model.Vendor

/**
 * Vendor Payout Dialog for paying vendors directly from the till.
 * 
 * Per FUNCTIONS_MENU.md (Vendor Payout section):
 * - Pay vendors directly from the till
 * - Prerequisites: No active payments in current transaction
 * - Flow: Select vendor → Enter amount → Manager approval → Cash dispensed
 * 
 * This is a two-step dialog:
 * - Step 1: Vendor List (Grid of vendor buttons)
 * - Step 2: Amount Input (TenKey)
 * 
 * @param step Current step of the dialog (VENDOR_SELECTION or AMOUNT_INPUT)
 * @param vendors List of available vendors
 * @param selectedVendor Currently selected vendor (null in step 1)
 * @param currentBalance Current drawer balance (formatted string)
 * @param inputValue Current input value for the amount
 * @param errorMessage Optional error message to display
 * @param onVendorSelect Called when a vendor is selected
 * @param onDigitClick Called when a digit is pressed
 * @param onClearClick Called when CLR is pressed
 * @param onBackspaceClick Called when backspace is pressed
 * @param onPayClick Called when Pay button is pressed
 * @param onBackClick Called when back button is pressed (step 2 -> step 1)
 * @param onDismiss Called when dialog should be dismissed
 */
@Composable
fun VendorPayoutDialog(
    step: VendorPayoutStep,
    vendors: List<Vendor>,
    selectedVendor: Vendor?,
    currentBalance: String,
    inputValue: String,
    errorMessage: String? = null,
    onVendorSelect: (Vendor) -> Unit,
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onPayClick: () -> Unit,
    onBackClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(500.dp)
                .testTag("vendor_payout_dialog")
        ) {
            Column {
                // Header
                VendorPayoutHeader(
                    step = step,
                    selectedVendor = selectedVendor,
                    onBack = onBackClick,
                    onClose = onDismiss
                )
                
                // Content with animation
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "vendor_payout_step"
                ) { currentStep ->
                    when (currentStep) {
                        VendorPayoutStep.VENDOR_SELECTION -> {
                            VendorSelectionContent(
                                vendors = vendors,
                                onVendorSelect = onVendorSelect,
                                onCancel = onDismiss
                            )
                        }
                        VendorPayoutStep.AMOUNT_INPUT -> {
                            AmountInputContent(
                                selectedVendor = selectedVendor,
                                currentBalance = currentBalance,
                                inputValue = inputValue,
                                errorMessage = errorMessage,
                                onDigitClick = onDigitClick,
                                onClearClick = onClearClick,
                                onBackspaceClick = onBackspaceClick,
                                onPayClick = onPayClick,
                                onCancel = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Steps in the Vendor Payout dialog flow.
 */
enum class VendorPayoutStep {
    /** Step 1: Select a vendor from the list */
    VENDOR_SELECTION,
    /** Step 2: Enter the payout amount */
    AMOUNT_INPUT
}

/**
 * Header for the Vendor Payout dialog.
 */
@Composable
private fun VendorPayoutHeader(
    step: VendorPayoutStep,
    selectedVendor: Vendor?,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GroPOSColors.PrimaryBlue)
            .padding(horizontal = GroPOSSpacing.S, vertical = GroPOSSpacing.S),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show back button only in step 2
            if (step == VendorPayoutStep.AMOUNT_INPUT) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            
            Column {
                Text(
                    text = "Vendor Payout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Show selected vendor in step 2
                if (step == VendorPayoutStep.AMOUNT_INPUT && selectedVendor != null) {
                    Text(
                        text = selectedVendor.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        
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
 * Step 1: Vendor selection content.
 */
@Composable
private fun VendorSelectionContent(
    vendors: List<Vendor>,
    onVendorSelect: (Vendor) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(GroPOSSpacing.M)
            .fillMaxWidth()
    ) {
        Text(
            text = "Select Vendor",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = GroPOSColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Vendor grid (2 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S),
            verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S),
            modifier = Modifier.height(200.dp)
        ) {
            items(vendors, key = { it.id }) { vendor ->
                VendorButton(
                    vendor = vendor,
                    onClick = { onVendorSelect(vendor) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Cancel button
        OutlineButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cancel_button")
        ) {
            Text("Cancel")
        }
    }
}

/**
 * Button for selecting a vendor.
 */
@Composable
private fun VendorButton(
    vendor: Vendor,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("vendor_button_${vendor.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.LightGray1,
            contentColor = Color.Black
        )
    ) {
        Text(
            text = vendor.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Step 2: Amount input content.
 */
@Composable
private fun AmountInputContent(
    selectedVendor: Vendor?,
    currentBalance: String,
    inputValue: String,
    errorMessage: String?,
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onPayClick: () -> Unit,
    onCancel: () -> Unit
) {
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
            text = "Enter payout amount",
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
                    modifier = Modifier.testTag("payout_amount_display")
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
            onOkClick = { onPayClick() },
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
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cancel_button")
            ) {
                Text("Cancel")
            }
            
            DangerButton(
                onClick = onPayClick,
                enabled = inputValue.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pay_button")
            ) {
                Text("Pay")
            }
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
        // Treat as cents (e.g., "5000" = "$50.00")
        val value = input.toLongOrNull() ?: 0
        val dollars = value / 100
        val cents = value % 100
        String.format("$%d.%02d", dollars, cents)
    }
}

