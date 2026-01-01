package com.unisight.gropos.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * GroPOS Ten-Key Numeric Keypad Component
 * 
 * Per COMPONENTS.md: The numeric keypad component used throughout the application
 * for barcode entry, quantity input, PIN entry, and price modification.
 * 
 * Layout:
 * ┌─────────────────────────────┐
 * │  [7]    [8]    [9]   [QTY]  │
 * │  [4]    [5]    [6]   [CLR]  │
 * │  [1]    [2]    [3]   [⌫]   │
 * │  [.]    [0]    [00]  [OK]   │
 * └─────────────────────────────┘
 */

// ============================================================================
// Ten-Key Modes
// ============================================================================

/**
 * Available modes for the Ten-Key component.
 */
sealed interface TenKeyMode {
    /** Standard numeric entry */
    data object Digit : TenKeyMode
    
    /** PIN entry for authentication */
    data object Login : TenKeyMode
    
    /** Quantity modification */
    data object Qty : TenKeyMode
    
    /** Refund amounts */
    data object Refund : TenKeyMode
    
    /** Price/weight prompts */
    data object Prompt : TenKeyMode
    
    /** Discount percentage */
    data object Discount : TenKeyMode
    
    /** Cash pickup amounts */
    data object CashPickup : TenKeyMode
    
    /** Vendor payout entry */
    data object Vendor : TenKeyMode
    
    /** Bag quantity */
    data object Bag : TenKeyMode
}

/**
 * State for the Ten-Key component.
 */
data class TenKeyState(
    val inputValue: String = "",
    val mode: TenKeyMode = TenKeyMode.Digit,
    val isFocused: Boolean = false
)

// ============================================================================
// Main Ten-Key Component
// ============================================================================

/**
 * Ten-Key numeric keypad.
 * 
 * @param state Current state of the keypad
 * @param onDigitClick Called when a digit button is pressed
 * @param onOkClick Called when OK button is pressed with current input value
 * @param onQtyClick Called when QTY button is pressed (optional)
 * @param onClearClick Called when CLR button is pressed
 * @param onBackspaceClick Called when backspace button is pressed
 * @param modifier Modifier for the component
 * @param showQtyButton Whether to show the QTY button (defaults to true)
 */
@Composable
fun TenKey(
    state: TenKeyState,
    onDigitClick: (String) -> Unit,
    onOkClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    onQtyClick: (() -> Unit)? = null,
    showQtyButton: Boolean = true
) {
    Surface(
        modifier = modifier,
        color = GroPOSColors.LightGray1,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(GroPOSSpacing.S),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: 7, 8, 9, QTY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TenKeyButton(
                    text = "7",
                    onClick = { onDigitClick("7") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "8",
                    onClick = { onDigitClick("8") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "9",
                    onClick = { onDigitClick("9") },
                    modifier = Modifier.weight(1f)
                )
                if (showQtyButton) {
                    TenKeySpecialButton(
                        text = "QTY",
                        onClick = { onQtyClick?.invoke() },
                        modifier = Modifier.weight(1f),
                        backgroundColor = GroPOSColors.PrimaryBlue
                    )
                } else {
                    TenKeyButton(
                        text = "",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false
                    )
                }
            }
            
            // Row 2: 4, 5, 6, CLR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TenKeyButton(
                    text = "4",
                    onClick = { onDigitClick("4") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "5",
                    onClick = { onDigitClick("5") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "6",
                    onClick = { onDigitClick("6") },
                    modifier = Modifier.weight(1f)
                )
                TenKeySpecialButton(
                    text = "CLR",
                    onClick = onClearClick,
                    modifier = Modifier.weight(1f),
                    backgroundColor = GroPOSColors.WarningOrange
                )
            }
            
            // Row 3: 1, 2, 3, Backspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TenKeyButton(
                    text = "1",
                    onClick = { onDigitClick("1") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "2",
                    onClick = { onDigitClick("2") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "3",
                    onClick = { onDigitClick("3") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyIconButton(
                    onClick = onBackspaceClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "⌫",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Black
                    )
                }
            }
            
            // Row 4: ., 0, 00, OK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TenKeyButton(
                    text = ".",
                    onClick = { onDigitClick(".") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "0",
                    onClick = { onDigitClick("0") },
                    modifier = Modifier.weight(1f)
                )
                TenKeyButton(
                    text = "00",
                    onClick = { onDigitClick("00") },
                    modifier = Modifier.weight(1f)
                )
                TenKeySpecialButton(
                    text = "OK",
                    onClick = { onOkClick(state.inputValue) },
                    modifier = Modifier.weight(1f),
                    backgroundColor = GroPOSColors.PrimaryGreen
                )
            }
        }
    }
}

// ============================================================================
// Internal Button Components
// ============================================================================

/**
 * Standard ten-key button (white background, black text).
 */
@Composable
private fun TenKeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.White,
            contentColor = Color.Black,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = GroPOSColors.TextSecondary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Special ten-key button (colored background).
 */
@Composable
private fun TenKeySpecialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GroPOSColors.PrimaryGreen
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Icon button for ten-key (e.g., backspace).
 */
@Composable
private fun TenKeyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.White,
            contentColor = Color.Black
        )
    ) {
        content()
    }
}

