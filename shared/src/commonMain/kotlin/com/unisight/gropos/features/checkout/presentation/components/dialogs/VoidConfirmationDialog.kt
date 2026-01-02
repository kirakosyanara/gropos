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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Confirmation dialog for voiding the entire transaction.
 * 
 * Per FUNCTIONS_MENU.md - Void Transaction:
 * - Cancels the current transaction entirely
 * - Requires explicit confirmation to prevent accidents
 * - Cannot be undone
 * 
 * @param onConfirm Called when user confirms void
 * @param onCancel Called when user cancels/dismisses
 */
@Composable
fun VoidConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = modifier.testTag("void_confirmation_dialog"),
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GroPOSSpacing.L),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = GroPOSColors.DangerRed,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Title
                Text(
                    text = "Void Transaction?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Body
                Text(
                    text = "Are you sure you want to void the entire transaction?\n\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                ) {
                    // Cancel Button (Secondary)
                    OutlineButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("void_cancel_button")
                    ) {
                        Text("No, Keep It")
                    }
                    
                    // Confirm Button (Danger/Red)
                    DangerButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("void_confirm_button")
                    ) {
                        Text(
                            text = "Yes, Void It",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

