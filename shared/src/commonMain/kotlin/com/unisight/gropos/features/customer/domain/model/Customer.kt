package com.unisight.gropos.features.customer.domain.model

import java.math.BigDecimal

/**
 * Customer model for loyalty program.
 * 
 * Per LOYALTY_PROGRAM.md: Customer information for discounts and tracking.
 * Per REMEDIATION_CHECKLIST: Info Bar - Customer Card.
 */
data class Customer(
    /** Unique customer ID */
    val id: Int,
    
    /** Customer's first name */
    val firstName: String,
    
    /** Customer's last name */
    val lastName: String,
    
    /** Email address */
    val email: String? = null,
    
    /** Phone number */
    val phone: String? = null,
    
    /** Loyalty card number */
    val loyaltyCardNumber: String? = null,
    
    /** Available store credit balance */
    val storeCreditBalance: BigDecimal = BigDecimal.ZERO,
    
    /** Account balance (for on-account payments) */
    val accountBalance: BigDecimal = BigDecimal.ZERO,
    
    /** Account credit limit */
    val accountCreditLimit: BigDecimal = BigDecimal.ZERO,
    
    /** Loyalty points balance */
    val loyaltyPoints: Int = 0,
    
    /** Loyalty tier (e.g., "Gold", "Silver", "Bronze") */
    val loyaltyTier: String? = null,
    
    /** Profile image URL */
    val imageUrl: String? = null,
    
    /** Whether the customer account is active */
    val isActive: Boolean = true,
    
    /** Customer notes (visible to cashier) */
    val notes: String? = null
) {
    /** Full name for display */
    val fullName: String
        get() = "$firstName $lastName"
    
    /** Initials for avatar display */
    val initials: String
        get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
    
    /** Whether customer has store credit available */
    val hasStoreCredit: Boolean
        get() = storeCreditBalance > BigDecimal.ZERO
    
    /** Whether customer has account charging enabled */
    val hasAccountCharging: Boolean
        get() = accountCreditLimit > BigDecimal.ZERO
    
    /** Available account credit */
    val availableAccountCredit: BigDecimal
        get() = accountCreditLimit - accountBalance
}

