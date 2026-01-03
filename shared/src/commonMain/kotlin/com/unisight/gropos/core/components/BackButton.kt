package com.unisight.gropos.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Consistent BackButton component for navigation.
 * 
 * Per REMEDIATION_CHECKLIST: BackButton - Consistent back navigation component.
 * Per UI_LAYOUT.md: Standard back navigation across all screens.
 * 
 * Variants:
 * - Icon only (compact)
 * - Icon with label
 * - Close button variant
 */
@Composable
fun NavigationBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Go back"
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(GroPOSColors.LightGray1)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = GroPOSColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Back button with text label.
 */
@Composable
fun BackButtonWithLabel(
    onClick: () -> Unit,
    label: String = "Back",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GroPOSColors.LightGray1)
            .clickable(onClick = onClick)
            .padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = GroPOSColors.TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = GroPOSColors.TextPrimary
        )
    }
}

/**
 * Close button for dialogs and modals.
 */
@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Close"
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(GroPOSColors.LightGray1)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = contentDescription,
            tint = GroPOSColors.TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Cancel button with red styling.
 */
@Composable
fun CancelButton(
    onClick: () -> Unit,
    label: String = "Cancel",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GroPOSColors.DangerRed.copy(alpha = 0.1f))
            .border(1.dp, GroPOSColors.DangerRed, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Cancel",
            tint = GroPOSColors.DangerRed,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = GroPOSColors.DangerRed
        )
    }
}

/**
 * Navigation header with back button and title.
 */
@Composable
fun NavigationHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(GroPOSSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
        ) {
            NavigationBackButton(onClick = onBackClick)
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
        }
        
        trailing?.invoke()
    }
}

/**
 * Custom back button with configurable icon.
 */
@Composable
fun CustomBackButton(
    onClick: () -> Unit,
    icon: ImageVector,
    backgroundColor: Color = GroPOSColors.LightGray1,
    iconColor: Color = GroPOSColors.TextPrimary,
    modifier: Modifier = Modifier,
    contentDescription: String = "Navigate"
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Floating back button for overlay screens.
 */
@Composable
fun FloatingBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(GroPOSColors.White)
            .border(1.dp, GroPOSColors.BorderGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Go back",
            tint = GroPOSColors.TextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

