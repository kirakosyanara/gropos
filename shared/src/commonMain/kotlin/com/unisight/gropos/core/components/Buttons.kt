package com.unisight.gropos.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius

/**
 * GroPOS Button Components
 * 
 * Per UI_DESIGN_SYSTEM.md: Button styles with consistent shape (20.dp radius),
 * colors, and padding.
 */

// ============================================================================
// Primary Buttons
// ============================================================================

/**
 * Success Button - Primary Green
 * 
 * Used for: Primary actions, success states, confirmations
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Container: PrimaryGreen (#04571B)
 * - Content: White
 * - Disabled Container: DisabledGray (#D9D9D9)
 * - Shape: RoundedCornerShape(20.dp)
 * - Padding: 14.dp
 */
@Composable
fun SuccessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.PrimaryGreen,
            contentColor = Color.White,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(14.dp),
        content = content
    )
}

/**
 * Primary Button - Blue
 * 
 * Used for: Primary alternative actions
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Container: PrimaryBlue (#2073BE)
 * - Content: White
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.PrimaryBlue,
            contentColor = Color.White,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(14.dp),
        content = content
    )
}

/**
 * Danger Button - Red
 * 
 * Used for: Cancel, delete, danger actions
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Container: DangerRed (#FA1B1B)
 * - Content: White
 */
@Composable
fun DangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.DangerRed,
            contentColor = Color.White,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(14.dp),
        content = content
    )
}

/**
 * Warning Button - Orange
 * 
 * Used for: Manager requests, lock button, alerts
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Container: WarningOrange (#FE793A)
 * - Content: White
 */
@Composable
fun WarningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.WarningOrange,
            contentColor = Color.White,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(14.dp),
        content = content
    )
}

// ============================================================================
// Secondary Buttons
// ============================================================================

/**
 * Outline Button - Gray Border
 * 
 * Used for: Secondary actions, back buttons
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Border: BorderGray (#857370)
 * - Content: PrimaryGreen
 * - Padding: horizontal 24.dp, vertical 10.dp
 */
@Composable
fun OutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        border = BorderStroke(1.dp, if (enabled) GroPOSColors.BorderGray else GroPOSColors.DisabledGray),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = GroPOSColors.PrimaryGreen,
            disabledContentColor = GroPOSColors.TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
        content = content
    )
}

/**
 * Back Button - Gray Border with Arrow Icon
 * 
 * Used for: Navigation back actions
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Border: BorderGray (#857370)
 * - Content: Back arrow icon + "Back" text
 * - Padding: horizontal 24.dp, vertical 18.dp
 */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        border = BorderStroke(1.dp, if (enabled) GroPOSColors.BorderGray else GroPOSColors.DisabledGray),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
        )
        Spacer(Modifier.width(8.dp))
        Text("Back")
    }
}

// ============================================================================
// Button Size Variants
// ============================================================================

/**
 * Large Button - Larger padding and text
 * 
 * Used for: Important actions that need more visual weight
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Padding: 14.dp
 * - Typography: titleLarge (24.sp)
 */
@Composable
fun LargeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = GroPOSColors.PrimaryGreen,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(14.dp)
    ) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge) {
            content()
        }
    }
}

/**
 * Extra Large Button - Even larger for critical actions
 * 
 * Used for: Primary transaction actions (Pay button)
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Padding: 18.dp
 * - Typography: titleLarge (24.sp)
 */
@Composable
fun ExtraLargeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = GroPOSColors.PrimaryGreen,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(GroPOSRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(18.dp)
    ) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge) {
            content()
        }
    }
}

