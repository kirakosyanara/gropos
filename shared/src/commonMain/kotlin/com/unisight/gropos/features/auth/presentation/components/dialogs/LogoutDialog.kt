package com.unisight.gropos.features.auth.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Logout options available to the user.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Lock Station: Keeps session active, requires PIN to re-enter
 * - Release Till: Unbinds cashier from drawer, freeing for another user
 * - End of Shift: Generates Z-Report, unbinds till, logs out
 */
enum class LogoutOption {
    /** Lock the station (keep session active) */
    LOCK_STATION,
    /** Release the till and logout (quick logout) */
    RELEASE_TILL,
    /** End the shift with report (full close-out) */
    END_OF_SHIFT
}

/**
 * Logout Dialog with three sign-out options.
 * 
 * Per DIALOGS.md and CASHIER_OPERATIONS.md:
 * - Header: "Sign Out Options"
 * - Option 1: Lock Station (Blue)
 * - Option 2: Release Till (Orange)
 * - Option 3: End of Shift (Red)
 * - Cancel button
 * 
 * @param onOptionSelected Called when user selects a logout option
 * @param onDismiss Called when dialog is dismissed
 * @param isCartEmpty Whether the cart is empty (affects Release/End options)
 */
@Composable
fun LogoutDialog(
    onOptionSelected: (LogoutOption) -> Unit,
    onDismiss: () -> Unit,
    isCartEmpty: Boolean = true,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("logout_dialog"),
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GroPOSSpacing.L)
            ) {
                // Header
                Text(
                    text = "Sign Out Options",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Option 1: Lock Station (Blue)
                LogoutOptionCard(
                    icon = Icons.Default.Lock,
                    title = "Lock Station",
                    description = "Lock this station. Requires PIN to re-enter.",
                    iconColor = GroPOSColors.PrimaryBlue,
                    backgroundColor = GroPOSColors.PrimaryBlue.copy(alpha = 0.1f),
                    enabled = true,
                    onClick = { onOptionSelected(LogoutOption.LOCK_STATION) },
                    testTag = "logout_lock_station"
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Option 2: Release Till (Orange)
                LogoutOptionCard(
                    icon = Icons.Default.Close,
                    title = "Release Till",
                    description = if (isCartEmpty) {
                        "Sign out and free this drawer for another user."
                    } else {
                        "⚠️ Clear cart before releasing till."
                    },
                    iconColor = GroPOSColors.WarningOrange,
                    backgroundColor = GroPOSColors.WarningOrange.copy(alpha = 0.1f),
                    enabled = isCartEmpty,
                    onClick = { onOptionSelected(LogoutOption.RELEASE_TILL) },
                    testTag = "logout_release_till"
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Option 3: End of Shift (Red)
                LogoutOptionCard(
                    icon = Icons.Default.ExitToApp,
                    title = "End of Shift",
                    description = if (isCartEmpty) {
                        "End your shift, print report, and count out."
                    } else {
                        "⚠️ Clear cart before ending shift."
                    },
                    iconColor = GroPOSColors.DangerRed,
                    backgroundColor = GroPOSColors.DangerRed.copy(alpha = 0.1f),
                    enabled = isCartEmpty,
                    onClick = { onOptionSelected(LogoutOption.END_OF_SHIFT) },
                    testTag = "logout_end_of_shift"
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Cancel Button
                OutlineButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("logout_cancel")
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Individual logout option card.
 */
@Composable
private fun LogoutOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.5f
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(GroPOSRadius.Small),
        color = backgroundColor.copy(alpha = alpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.2f * alpha),
                        shape = RoundedCornerShape(GroPOSRadius.Small)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = alpha),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(GroPOSSpacing.M))
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary.copy(alpha = alpha)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary.copy(alpha = alpha)
                )
            }
        }
    }
}

