package com.unisight.gropos.features.checkout.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.customer.domain.model.Customer

/**
 * Customer Info Bar Component
 * 
 * Per REMEDIATION_CHECKLIST: Info Bar - Customer Card.
 * Displays customer avatar, name, loyalty tier, and search button.
 * 
 * States:
 * - No customer selected: Shows search prompt
 * - Customer selected: Shows avatar, name, tier, points, clear button
 */
@Composable
fun CustomerInfoBar(
    customer: Customer?,
    onSearchClick: () -> Unit,
    onClearCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GroPOSColors.LightGray2,
        shape = RoundedCornerShape(GroPOSRadius.Small)
    ) {
        if (customer != null) {
            // Customer selected view
            CustomerSelectedView(
                customer = customer,
                onClearClick = onClearCustomer
            )
        } else {
            // No customer view - search prompt
            NoCustomerView(onSearchClick = onSearchClick)
        }
    }
}

@Composable
private fun CustomerSelectedView(
    customer: Customer,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(GroPOSSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            // Avatar
            CustomerAvatar(
                initials = customer.initials,
                imageUrl = customer.imageUrl,
                loyaltyTier = customer.loyaltyTier
            )
            
            // Name and tier
            Column {
                Text(
                    text = customer.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.XS)
                ) {
                    // Loyalty tier badge
                    customer.loyaltyTier?.let { tier ->
                        LoyaltyTierBadge(tier = tier)
                    }
                    
                    // Points
                    if (customer.loyaltyPoints > 0) {
                        Text(
                            text = "${customer.loyaltyPoints} pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                    
                    // Store credit indicator
                    if (customer.hasStoreCredit) {
                        Text(
                            text = "• Credit available",
                            style = MaterialTheme.typography.bodySmall,
                            color = GroPOSColors.PrimaryGreen
                        )
                    }
                }
            }
        }
        
        // Clear button
        IconButton(onClick = onClearClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear customer",
                tint = GroPOSColors.TextSecondary
            )
        }
    }
}

@Composable
private fun NoCustomerView(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSearchClick)
            .padding(GroPOSSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = GroPOSColors.TextSecondary
        )
        Spacer(modifier = Modifier.width(GroPOSSpacing.S))
        Text(
            text = "Search customer / Scan loyalty card",
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary
        )
    }
}

@Composable
private fun CustomerAvatar(
    initials: String,
    imageUrl: String?,
    loyaltyTier: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(getTierColor(loyaltyTier)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                // TODO: Load image from URL
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GroPOSColors.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.White
                )
            }
        }
        
        // Tier indicator star for Gold/Platinum
        if (loyaltyTier in listOf("Gold", "Platinum")) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = loyaltyTier,
                tint = GroPOSColors.WarningOrange,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun LoyaltyTierBadge(
    tier: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Pill),
        color = getTierColor(tier).copy(alpha = 0.2f)
    ) {
        Text(
            text = tier,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = getTierColor(tier),
            modifier = Modifier.padding(horizontal = GroPOSSpacing.S, vertical = 2.dp)
        )
    }
}

/**
 * Gets the color for a loyalty tier.
 */
private fun getTierColor(tier: String?): androidx.compose.ui.graphics.Color {
    return when (tier?.lowercase()) {
        "platinum" -> GroPOSColors.TextPrimary
        "gold" -> GroPOSColors.WarningOrange
        "silver" -> GroPOSColors.TextSecondary
        "bronze" -> GroPOSColors.SavingsRed
        else -> GroPOSColors.PrimaryGreen
    }
}

