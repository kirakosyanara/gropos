package com.unisight.gropos.features.customer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSSpacing
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * SNAP Eligible Display Component.
 * 
 * Per REMEDIATION_CHECKLIST: SNAP Eligible Display - Add to customer display totals panel.
 * Per GOVERNMENT_BENEFITS.md: Display SNAP-eligible total for EBT customers.
 * 
 * Shows:
 * - Total amount eligible for SNAP (food stamps)
 * - Visual indicator for EBT payment
 * - Helpful for cashiers and customers
 */
@Composable
fun SnapEligibleDisplay(
    snapEligibleAmount: BigDecimal,
    totalAmount: BigDecimal,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val nonSnapAmount = totalAmount - snapEligibleAmount
    
    val hasSnapItems = snapEligibleAmount > BigDecimal.ZERO
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (hasSnapItems) GroPOSColors.LightGray1 else Color.Transparent)
            .border(
                width = if (hasSnapItems) 2.dp else 0.dp,
                color = if (hasSnapItems) SnapGreen else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(GroPOSSpacing.M),
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        // Header with EBT icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = "SNAP/EBT",
                tint = SnapGreen,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = "EBT/SNAP Eligibility",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
        }
        
        // SNAP Eligible Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SnapGreen)
                )
                Text(
                    text = "SNAP Eligible:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary
                )
            }
            
            Text(
                text = currencyFormatter.format(snapEligibleAmount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = SnapGreen
            )
        }
        
        // Non-SNAP Amount (if any)
        if (nonSnapAmount > BigDecimal.ZERO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(GroPOSColors.TextSecondary)
                    )
                    Text(
                        text = "Non-SNAP:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.TextSecondary
                    )
                }
                
                Text(
                    text = currencyFormatter.format(nonSnapAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Compact SNAP indicator for order cells.
 * 
 * Shows a small badge indicating an item is SNAP-eligible.
 */
@Composable
fun SnapEligibleBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SnapGreen.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = "SNAP",
            tint = SnapGreen,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "SNAP",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SnapGreen
        )
    }
}

/**
 * EBT Cash Eligible indicator.
 */
@Composable
fun EbtCashEligibleBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(EbtCashBlue.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "EBT Cash",
            tint = EbtCashBlue,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "EBT$",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = EbtCashBlue
        )
    }
}

/**
 * Combined EBT summary for customer display.
 */
@Composable
fun EbtSummaryPanel(
    snapEligible: BigDecimal,
    ebtCashEligible: BigDecimal,
    total: BigDecimal,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val nonEbtAmount = total - snapEligible - ebtCashEligible
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GroPOSColors.LightGray1)
            .padding(GroPOSSpacing.M),
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        Text(
            text = "Payment Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary
        )
        
        // SNAP eligible
        if (snapEligible > BigDecimal.ZERO) {
            EbtLineItem(
                label = "SNAP Eligible",
                amount = snapEligible,
                color = SnapGreen
            )
        }
        
        // EBT Cash eligible
        if (ebtCashEligible > BigDecimal.ZERO) {
            EbtLineItem(
                label = "EBT Cash Eligible",
                amount = ebtCashEligible,
                color = EbtCashBlue
            )
        }
        
        // Non-EBT
        if (nonEbtAmount > BigDecimal.ZERO) {
            EbtLineItem(
                label = "Other Payment",
                amount = nonEbtAmount,
                color = GroPOSColors.TextSecondary
            )
        }
        
        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GroPOSColors.BorderGray)
        )
        
        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Due",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
            Text(
                text = currencyFormatter.format(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
        }
    }
}

@Composable
private fun EbtLineItem(
    label: String,
    amount: BigDecimal,
    color: Color
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.TextSecondary
            )
        }
        Text(
            text = currencyFormatter.format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// SNAP/EBT brand colors
private val SnapGreen = Color(0xFF228B22) // Forest Green for SNAP
private val EbtCashBlue = Color(0xFF1E90FF) // Dodger Blue for EBT Cash

