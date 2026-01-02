package com.unisight.gropos.core.components.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.unisight.gropos.core.theme.GroPOSColors

/**
 * State for the Error Dialog.
 * 
 * Per DIALOGS.md (Error Message Dialog):
 * - Red error icon
 * - Centered message text
 * - OK/Dismiss button
 * 
 * Use for CRITICAL errors that STOP THE FLOW:
 * - Payment Declined
 * - Printer Error
 * - Hardware Failure
 * - Age Verification Failed (legal clarity)
 * 
 * Do NOT use for non-critical errors like "Product Not Found" - use Snackbar instead.
 * 
 * @property title Dialog title (displayed in red header)
 * @property message Error message body text
 * @property actionLabel Custom action button label (default: "Dismiss")
 */
data class ErrorDialogState(
    val title: String = "Error",
    val message: String = "",
    val actionLabel: String = "Dismiss"
)

/**
 * Error Dialog - Critical Alert System.
 * 
 * Per DIALOGS.md (Error Message Dialog section):
 * - Icon: Error icon in danger red color
 * - Title: Bold, customizable (default "Error")
 * - Message: Body text, centered
 * - Button: Dismiss (or custom action label)
 * - Z-Index: High (must sit above everything else)
 * 
 * Usage Governance:
 * - Only use for errors that STOP THE FLOW (Payments, Hardware, Legal)
 * - Minor errors like "Item not found" should use Snackbar
 * - User must be able to dismiss it to return to sales screen
 * 
 * @param state The error dialog state containing title, message, and action label
 * @param onDismiss Callback when the dialog is dismissed
 * @param modifier Optional modifier for the dialog
 */
@Composable
fun ErrorDialog(
    state: ErrorDialogState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false, // Critical errors require explicit dismissal
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .width(450.dp)
                .zIndex(1000f) // High z-index to sit above everything
                .testTag("error_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp // Elevated to stand out
        ) {
            Column {
                // Red Header (per DIALOGS.md: Error dialogs use red/danger styling)
                ErrorDialogHeader(
                    title = state.title,
                    onClose = onDismiss
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Error Icon (using Warning icon styled as error)
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Error",
                        tint = GroPOSColors.DangerRed,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("error_dialog_icon")
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Message
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = GroPOSColors.TextPrimary,
                        modifier = Modifier.testTag("error_dialog_message")
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Dismiss Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("error_dialog_dismiss_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GroPOSColors.PrimaryBlue // Blue per DIALOGS.md
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = state.actionLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Error dialog header with red background.
 * 
 * Per DIALOGS.md: Error dialogs use danger red (#FA1B1B) header.
 */
@Composable
private fun ErrorDialogHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GroPOSColors.DangerRed)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("error_dialog_title")
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

