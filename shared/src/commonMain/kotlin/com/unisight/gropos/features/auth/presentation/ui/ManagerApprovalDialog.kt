package com.unisight.gropos.features.auth.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Manager approval dialog for privileged operations.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Required for "Force Sign Out" from lock screen
 * - Manager enters their PIN to approve the action
 * - Requires permission: "GrowPOS.Store.Force Sign Out"
 * 
 * @param actionDescription Description of the action requiring approval
 * @param onApprove Called with manager PIN when approve is clicked
 * @param onDismiss Called when dialog is dismissed
 * @param isVerifying True while verifying the manager's PIN
 * @param errorMessage Error message to display (e.g., "Invalid PIN")
 */
@Composable
fun ManagerApprovalDialog(
    actionDescription: String,
    onApprove: (managerPin: String) -> Unit,
    onDismiss: () -> Unit,
    isVerifying: Boolean = false,
    errorMessage: String? = null
) {
    var managerPin by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Manager Approval Required",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Manager Approval Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = actionDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = managerPin,
                    onValueChange = { 
                        if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                            managerPin = it 
                        }
                    },
                    label = { Text("Manager PIN") },
                    placeholder = { Text("Enter manager PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    enabled = !isVerifying,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isVerifying) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verifying...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApprove(managerPin) },
                enabled = managerPin.length >= 4 && !isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying
            ) {
                Text("Cancel")
            }
        }
    )
}

