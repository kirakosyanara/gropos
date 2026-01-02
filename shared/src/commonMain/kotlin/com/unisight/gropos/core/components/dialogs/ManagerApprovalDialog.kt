package com.unisight.gropos.core.components.dialogs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyMode
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.security.ManagerInfo
import com.unisight.gropos.core.security.RequestAction
import com.unisight.gropos.core.theme.GroPOSColors
import java.math.BigDecimal

/**
 * State for the Manager Approval Dialog.
 */
enum class ApprovalDialogStep {
    SELECT_MANAGER,
    ENTER_PIN,
    PROCESSING
}

/**
 * Manager Approval Dialog.
 *
 * Per ROLES_AND_PERMISSIONS.md and DIALOGS.md:
 * - Two-step flow: Select Manager -> Enter PIN
 * - Shows action type and amount
 * - Uses TenKey for PIN entry
 *
 * @param action The action requiring approval
 * @param amount Optional amount for display
 * @param managers List of available managers
 * @param onApproved Callback when approval succeeds (managerId, approvalCode)
 * @param onDenied Callback when approval is denied
 * @param onCancel Callback when user cancels
 */
@Composable
fun ManagerApprovalDialog(
    action: RequestAction,
    amount: BigDecimal? = null,
    managers: List<ManagerInfo>,
    isProcessing: Boolean = false,
    errorMessage: String? = null,
    onApproved: (Int, String) -> Unit,
    onDenied: () -> Unit,
    onCancel: () -> Unit,
    onPinSubmit: (managerId: Int, pin: String) -> Unit
) {
    var step by remember { mutableStateOf(ApprovalDialogStep.SELECT_MANAGER) }
    var selectedManager by remember { mutableStateOf<ManagerInfo?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    
    // Reset error when step changes
    if (step != ApprovalDialogStep.ENTER_PIN) {
        localError = null
    }
    
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .testTag("manager_approval_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header
                DialogHeader(
                    title = "Manager Approval Required",
                    onClose = onCancel
                )
                
                // Content
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Request Info
                    RequestInfoSection(action = action, amount = amount)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when {
                        isProcessing -> {
                            ProcessingState()
                        }
                        step == ApprovalDialogStep.SELECT_MANAGER -> {
                            ManagerSelectionSection(
                                managers = managers,
                                onManagerSelected = { manager ->
                                    selectedManager = manager
                                    pinInput = ""
                                    step = ApprovalDialogStep.ENTER_PIN
                                }
                            )
                        }
                        step == ApprovalDialogStep.ENTER_PIN -> {
                            selectedManager?.let { manager ->
                                PinEntrySection(
                                    manager = manager,
                                    pinInput = pinInput,
                                    errorMessage = errorMessage ?: localError,
                                    onPinChange = { newPin -> 
                                        pinInput = newPin
                                        localError = null
                                    },
                                    onSubmit = {
                                        if (pinInput.length >= 4) {
                                            onPinSubmit(manager.id, pinInput)
                                        } else {
                                            localError = "PIN must be at least 4 digits"
                                        }
                                    },
                                    onBack = {
                                        step = ApprovalDialogStep.SELECT_MANAGER
                                        pinInput = ""
                                    }
                                )
                            }
                        }
                    }
                    
                    // Cancel button
                    if (!isProcessing && step == ApprovalDialogStep.SELECT_MANAGER) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlineButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog header with title and close button.
 *
 * Per DIALOGS.md: Green header (#04571B) with white text.
 */
@Composable
private fun DialogHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF04571B))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

/**
 * Section showing the request type and amount.
 */
@Composable
private fun RequestInfoSection(
    action: RequestAction,
    amount: BigDecimal?
) {
    Column {
        Text(
            text = "Action Requiring Approval:",
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = action.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary
        )
        if (amount != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Amount: $${amount.setScale(2)}",
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.TextPrimary
            )
        }
    }
}

/**
 * Section for selecting a manager.
 */
@Composable
private fun ManagerSelectionSection(
    managers: List<ManagerInfo>,
    onManagerSelected: (ManagerInfo) -> Unit
) {
    Column {
        Text(
            text = "Select a manager to approve:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (managers.isEmpty()) {
            Text(
                text = "No managers available for this action.",
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.DangerRed,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(200.dp)
            ) {
                items(managers) { manager ->
                    ManagerListItem(
                        manager = manager,
                        onClick = { onManagerSelected(manager) }
                    )
                }
            }
        }
    }
}

/**
 * Single manager item in the selection list.
 */
@Composable
private fun ManagerListItem(
    manager: ManagerInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("manager_item_${manager.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF04571B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = manager.fullName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = manager.jobTitle ?: manager.role,
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.TextSecondary
            )
        }
    }
    HorizontalDivider()
}

/**
 * Section for entering manager PIN.
 */
@Composable
private fun PinEntrySection(
    manager: ManagerInfo,
    pinInput: String,
    errorMessage: String?,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selected manager info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF04571B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = manager.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = manager.jobTitle ?: manager.role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary
                )
            }
        }
        
        Text(
            text = "Enter Manager PIN:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // PIN display
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .background(
                            if (index < pinInput.length) GroPOSColors.PrimaryGreen
                            else Color(0xFFE0E0E0),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < pinInput.length) {
                        Text(
                            text = "●",
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
        
        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = GroPOSColors.DangerRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // TenKey
        TenKey(
            state = TenKeyState(
                inputValue = "",
                mode = TenKeyMode.Login
            ),
            onDigitClick = { digit ->
                if (pinInput.length < 6) {
                    onPinChange(pinInput + digit)
                }
            },
            onClearClick = { onPinChange("") },
            onBackspaceClick = {
                if (pinInput.isNotEmpty()) {
                    onPinChange(pinInput.dropLast(1))
                }
            },
            onOkClick = { onSubmit() },
            showQtyButton = false,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlineButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            SuccessButton(
                onClick = onSubmit,
                enabled = pinInput.length >= 4,
                modifier = Modifier.weight(1f)
            ) {
                Text("Approve")
            }
        }
    }
}

/**
 * Processing state with spinner.
 */
@Composable
private fun ProcessingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = GroPOSColors.PrimaryGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Validating...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

