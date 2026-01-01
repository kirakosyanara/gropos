package com.unisight.gropos.core.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSIconSize
import com.unisight.gropos.core.theme.GroPOSRadius

/**
 * GroPOS Form Field Components
 * 
 * Per UI_DESIGN_SYSTEM.md: Form elements with consistent styling.
 */

// ============================================================================
// Search Field
// ============================================================================

/**
 * Search Field with leading search icon.
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Shape: RoundedCornerShape(3.dp) [Small radius]
 * - Icon: 35.dp (Medium)
 * - Border: Gray when unfocused
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(GroPOSIconSize.Medium)
            )
        },
        shape = RoundedCornerShape(GroPOSRadius.Small),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Gray,
            focusedBorderColor = GroPOSColors.PrimaryGreen,
            cursorColor = GroPOSColors.PrimaryGreen
        ),
        singleLine = true
    )
}

// ============================================================================
// Quantity Field
// ============================================================================

/**
 * Quantity Field for numeric input.
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Shape: RoundedCornerShape(3.dp) [Small radius]
 * - Width: 75.dp
 * - Text alignment: Center
 */
@Composable
fun QuantityField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow numeric input
            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                onValueChange(newValue)
            }
        },
        modifier = modifier.width(75.dp),
        enabled = enabled,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        shape = RoundedCornerShape(GroPOSRadius.Small),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = GroPOSColors.BorderGray,
            focusedBorderColor = GroPOSColors.PrimaryGreen,
            cursorColor = GroPOSColors.PrimaryGreen
        )
    )
}

// ============================================================================
// Password Field
// ============================================================================

/**
 * Password Field with visibility toggle.
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Visibility toggle icon
 * - Text alignment: Center
 * - Keyboard type: NumberPassword
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "PIN"
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = { Text(label) },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(
                    text = if (passwordVisible) "👁" else "👁‍🗨",
                    style = LocalTextStyle.current
                )
            }
        },
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = GroPOSColors.BorderGray,
            focusedBorderColor = GroPOSColors.PrimaryGreen,
            cursorColor = GroPOSColors.PrimaryGreen
        )
    )
}

// ============================================================================
// Barcode Input Field
// ============================================================================

/**
 * Barcode Input Field for manual entry.
 * 
 * Used for: Manual barcode/PLU entry
 */
@Composable
fun BarcodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Scan or enter barcode...",
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = { Text(placeholder) },
        shape = RoundedCornerShape(GroPOSRadius.Small),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = GroPOSColors.BorderGray,
            focusedBorderColor = GroPOSColors.PrimaryGreen,
            cursorColor = GroPOSColors.PrimaryGreen
        )
    )
}

