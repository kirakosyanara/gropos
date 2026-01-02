package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyMode
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.features.checkout.presentation.AgeVerificationDialogState
import com.unisight.gropos.features.checkout.presentation.DateField

/**
 * Age Verification Dialog.
 * 
 * Per DIALOGS.md (Age Verification section):
 * - Triggered when scanning age-restricted products (alcohol, tobacco)
 * - Title: "Age Verification Required"
 * - Body: "Item: {ProductName} (Minimum Age: {Age})"
 * - Input: DateInput (MM/DD/YYYY) using TenKey
 * - Computed: Show "Current Age: XX" dynamically as they type
 * - Actions: "Cancel", "Confirm" (Enabled only if Age >= Limit)
 * - Manager Override button for special cases
 * 
 * Per Governance: The item MUST NOT be added to cart until dialog is confirmed.
 * 
 * @param state The current dialog state
 * @param onDigitClick Called when a digit is pressed on the TenKey
 * @param onClear Called when CLR is pressed
 * @param onBackspace Called when backspace is pressed
 * @param onFieldSelect Called when a date field (MM/DD/YYYY) is selected
 * @param onConfirm Called when verification succeeds
 * @param onCancel Called when user cancels
 * @param onManagerOverride Called when manager override is requested
 */
@Composable
fun AgeVerificationDialog(
    state: AgeVerificationDialogState,
    onDigitClick: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onFieldSelect: (DateField) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onManagerOverride: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .testTag("age_verification_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header
                DialogHeader(
                    title = "Age Verification Required",
                    onClose = onCancel
                )
                
                // Content
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Warning Icon
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),  // Orange warning color
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Product Info
                    Text(
                        text = "This product requires age verification",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Item: ${state.productName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Customer must be ${state.requiredAge}+ years old",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.DangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Date of Birth Input
                    Text(
                        text = "Enter Customer's Date of Birth:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Date Input Fields (MM / DD / YYYY)
                    DateInputRow(
                        monthInput = state.monthInput,
                        dayInput = state.dayInput,
                        yearInput = state.yearInput,
                        activeField = state.activeField,
                        onFieldSelect = onFieldSelect
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Calculated Age Display
                    if (state.calculatedAge != null) {
                        val ageColor = if (state.meetsAgeRequirement) {
                            GroPOSColors.PrimaryGreen
                        } else {
                            GroPOSColors.DangerRed
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Age: ",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${state.calculatedAge} years",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ageColor
                            )
                        }
                        
                        if (!state.meetsAgeRequirement) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Customer does not meet age requirement",
                                style = MaterialTheme.typography.bodySmall,
                                color = GroPOSColors.DangerRed
                            )
                        }
                    }
                    
                    // Error Message
                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GroPOSColors.DangerRed,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // TenKey for Date Entry
                    if (!state.isProcessing) {
                        TenKey(
                            state = TenKeyState(
                                inputValue = "",
                                mode = TenKeyMode.Digit
                            ),
                            onDigitClick = onDigitClick,
                            onClearClick = onClear,
                            onBackspaceClick = onBackspace,
                            onOkClick = { if (state.meetsAgeRequirement) onConfirm() },
                            showQtyButton = false,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Processing state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Processing override...")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    if (!state.isProcessing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            OutlineButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            
                            // Confirm Button (enabled only if age verified)
                            SuccessButton(
                                onClick = onConfirm,
                                enabled = state.meetsAgeRequirement,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Manager Override Button
                        Button(
                            onClick = onManagerOverride,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GroPOSColors.PrimaryBlue
                            )
                        ) {
                            Text("Manager Override")
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
 * Row of date input fields (MM / DD / YYYY).
 * 
 * Each field is a clickable box that shows the current input.
 * The active field is highlighted with a colored border.
 */
@Composable
private fun DateInputRow(
    monthInput: String,
    dayInput: String,
    yearInput: String,
    activeField: DateField,
    onFieldSelect: (DateField) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Month Field
        DateFieldBox(
            value = monthInput,
            placeholder = "MM",
            maxLength = 2,
            isActive = activeField == DateField.MONTH,
            onClick = { onFieldSelect(DateField.MONTH) },
            testTag = "dob_month"
        )
        
        Text(
            text = " / ",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Day Field
        DateFieldBox(
            value = dayInput,
            placeholder = "DD",
            maxLength = 2,
            isActive = activeField == DateField.DAY,
            onClick = { onFieldSelect(DateField.DAY) },
            testTag = "dob_day"
        )
        
        Text(
            text = " / ",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Year Field
        DateFieldBox(
            value = yearInput,
            placeholder = "YYYY",
            maxLength = 4,
            isActive = activeField == DateField.YEAR,
            onClick = { onFieldSelect(DateField.YEAR) },
            testTag = "dob_year"
        )
    }
}

/**
 * Individual date field input box.
 */
@Composable
private fun DateFieldBox(
    value: String,
    placeholder: String,
    maxLength: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isActive) GroPOSColors.PrimaryGreen else Color.LightGray
    val borderWidth = if (isActive) 2.dp else 1.dp
    val backgroundColor = if (isActive) Color(0xFFE8F5E9) else Color.White
    
    // Width based on field type
    val fieldWidth = if (maxLength == 4) 80.dp else 56.dp
    
    Box(
        modifier = Modifier
            .width(fieldWidth)
            .height(56.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.ifEmpty { placeholder },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (value.isEmpty()) Color.Gray else Color.Black,
            fontSize = if (maxLength == 4) 20.sp else 24.sp
        )
    }
}

