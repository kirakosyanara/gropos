package com.unisight.gropos.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * GroPOS Container Components
 * 
 * Per UI_DESIGN_SYSTEM.md: Page sections and card containers.
 */

// ============================================================================
// Page Sections (Split Layouts)
// ============================================================================

/**
 * Left Block - Left panel in split layouts
 * 
 * Used for: Order list, main content area
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Background: LightGray2 (#E1E3E3)
 * - Padding: 38.dp (XXL)
 */
@Composable
fun LeftBlock(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(GroPOSColors.LightGray2)
            .padding(GroPOSSpacing.XXL),
        content = content
    )
}

/**
 * Right Block - Right panel in split layouts
 * 
 * Used for: Actions panel, totals, ten-key
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Background: LightGray1 (#EFF1F1)
 * - Padding: horizontal 48.dp (XXXL), vertical 8.dp
 */
@Composable
fun RightBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(GroPOSColors.LightGray1)
            .padding(horizontal = GroPOSSpacing.XXXL, vertical = 8.dp),
        content = content
    )
}

// ============================================================================
// Cards and Containers
// ============================================================================

/**
 * White Box - Standard info card
 * 
 * Used for: Information cards, content containers
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Background: White (#FFFFFF)
 * - Shape: RoundedCornerShape(16.dp)
 * - Padding: 14.dp
 */
@Composable
fun WhiteBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = GroPOSColors.White
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

/**
 * Green Box - Highlight/success card
 * 
 * Used for: Highlighted content, success states
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Background: PrimaryGreen (#04571B)
 * - Shape: RoundedCornerShape(16.dp)
 * - Padding: 14.dp
 */
@Composable
fun GreenBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = GroPOSColors.PrimaryGreen
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

/**
 * Card Box - Generic card with customizable background
 * 
 * Used for: Various card containers
 */
@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GroPOSColors.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

// ============================================================================
// Status Indicators
// ============================================================================

/**
 * Request Status Box - Orange status indicator
 * 
 * Used for: Manager request notifications, pending actions
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Background: WarningOrange (#FE793A)
 * - Shape: RoundedCornerShape(30.dp)
 * - Text: White, bodyLarge
 */
@Composable
fun RequestStatusBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = GroPOSColors.WarningOrange
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            content = content
        )
    }
}

/**
 * Status Text Box - Green status indicator
 * 
 * Used for: Active status display
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Background: PrimaryGreen (#04571B)
 * - Padding: horizontal 10.dp, vertical 15.dp
 */
@Composable
fun StatusTextBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = GroPOSColors.PrimaryGreen
    ) {
        Column(
            modifier = Modifier.padding(horizontal = GroPOSSpacing.S, vertical = 15.dp),
            content = content
        )
    }
}

