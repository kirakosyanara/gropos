package com.unisight.gropos.core.security

import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import java.math.BigDecimal

/**
 * Permission Thresholds for various actions.
 * 
 * Per REMEDIATION_CHECKLIST: Permission Thresholds (discount limits, return limits).
 * Per ROLES_AND_PERMISSIONS.md: Threshold-based permission checking.
 * 
 * Thresholds define maximum amounts employees can authorize at their level.
 * Exceeding a threshold requires manager approval.
 */
data class PermissionThresholds(
    /** Maximum line item discount percentage without approval */
    val maxLineDiscountPercent: Int = 10,
    
    /** Maximum transaction discount percentage without approval */
    val maxTransactionDiscountPercent: Int = 5,
    
    /** Maximum return amount without approval */
    val maxReturnAmount: BigDecimal = BigDecimal("50.00"),
    
    /** Maximum void transaction amount without approval */
    val maxVoidAmount: BigDecimal = BigDecimal("100.00"),
    
    /** Maximum cash pickup amount without dual verification */
    val maxCashPickupAmount: BigDecimal = BigDecimal("500.00"),
    
    /** Maximum add cash amount without approval */
    val maxAddCashAmount: BigDecimal = BigDecimal("100.00"),
    
    /** Maximum price override below floor without approval */
    val allowFloorPriceOverride: Boolean = false,
    
    /** Maximum lottery payout without approval */
    val maxLotteryPayoutAmount: BigDecimal = BigDecimal("100.00")
) {
    companion object {
        /**
         * Default thresholds for Cashier role (Level 1).
         */
        val CASHIER = PermissionThresholds(
            maxLineDiscountPercent = 5,
            maxTransactionDiscountPercent = 0,
            maxReturnAmount = BigDecimal("25.00"),
            maxVoidAmount = BigDecimal.ZERO,
            maxCashPickupAmount = BigDecimal.ZERO,
            maxAddCashAmount = BigDecimal("50.00"),
            allowFloorPriceOverride = false,
            maxLotteryPayoutAmount = BigDecimal("100.00")
        )
        
        /**
         * Default thresholds for Supervisor role (Level 3).
         */
        val SUPERVISOR = PermissionThresholds(
            maxLineDiscountPercent = 15,
            maxTransactionDiscountPercent = 10,
            maxReturnAmount = BigDecimal("100.00"),
            maxVoidAmount = BigDecimal("50.00"),
            maxCashPickupAmount = BigDecimal("200.00"),
            maxAddCashAmount = BigDecimal("200.00"),
            allowFloorPriceOverride = false,
            maxLotteryPayoutAmount = BigDecimal("500.00")
        )
        
        /**
         * Default thresholds for Manager role (Level 4).
         */
        val MANAGER = PermissionThresholds(
            maxLineDiscountPercent = 50,
            maxTransactionDiscountPercent = 25,
            maxReturnAmount = BigDecimal("500.00"),
            maxVoidAmount = BigDecimal("500.00"),
            maxCashPickupAmount = BigDecimal("1000.00"),
            maxAddCashAmount = BigDecimal("500.00"),
            allowFloorPriceOverride = true,
            maxLotteryPayoutAmount = BigDecimal("5000.00")
        )
        
        /**
         * No limits for Admin role (Level 5).
         */
        val ADMIN = PermissionThresholds(
            maxLineDiscountPercent = 100,
            maxTransactionDiscountPercent = 100,
            maxReturnAmount = BigDecimal("99999.99"),
            maxVoidAmount = BigDecimal("99999.99"),
            maxCashPickupAmount = BigDecimal("99999.99"),
            maxAddCashAmount = BigDecimal("99999.99"),
            allowFloorPriceOverride = true,
            maxLotteryPayoutAmount = BigDecimal("99999.99")
        )
        
        /**
         * Gets the thresholds for a given user role.
         */
        fun forRole(role: UserRole): PermissionThresholds {
            return when (role) {
                UserRole.CASHIER -> CASHIER
                UserRole.SUPERVISOR -> SUPERVISOR
                UserRole.MANAGER -> MANAGER
                UserRole.ADMIN -> ADMIN
            }
        }
    }
}

/**
 * Result of a threshold check.
 */
sealed class ThresholdCheckResult {
    /** Action is within threshold, no approval needed */
    data object Allowed : ThresholdCheckResult()
    
    /** Action exceeds threshold, approval required */
    data class RequiresApproval(
        val reason: String,
        val requestedAmount: BigDecimal,
        val maxAllowed: BigDecimal
    ) : ThresholdCheckResult()
    
    /** Action is not allowed at all for this role */
    data class Denied(val reason: String) : ThresholdCheckResult()
}

/**
 * Service for checking permission thresholds.
 */
object ThresholdChecker {
    
    /**
     * Checks if a line item discount is within threshold.
     */
    fun checkLineDiscount(user: AuthUser, discountPercent: Int): ThresholdCheckResult {
        val thresholds = PermissionThresholds.forRole(user.role)
        
        return when {
            discountPercent <= thresholds.maxLineDiscountPercent -> ThresholdCheckResult.Allowed
            discountPercent <= 100 -> ThresholdCheckResult.RequiresApproval(
                reason = "Line discount exceeds ${thresholds.maxLineDiscountPercent}% limit",
                requestedAmount = BigDecimal(discountPercent),
                maxAllowed = BigDecimal(thresholds.maxLineDiscountPercent)
            )
            else -> ThresholdCheckResult.Denied("Discount cannot exceed 100%")
        }
    }
    
    /**
     * Checks if a transaction discount is within threshold.
     */
    fun checkTransactionDiscount(user: AuthUser, discountPercent: Int): ThresholdCheckResult {
        val thresholds = PermissionThresholds.forRole(user.role)
        
        return when {
            discountPercent <= thresholds.maxTransactionDiscountPercent -> ThresholdCheckResult.Allowed
            discountPercent <= 100 -> ThresholdCheckResult.RequiresApproval(
                reason = "Transaction discount exceeds ${thresholds.maxTransactionDiscountPercent}% limit",
                requestedAmount = BigDecimal(discountPercent),
                maxAllowed = BigDecimal(thresholds.maxTransactionDiscountPercent)
            )
            else -> ThresholdCheckResult.Denied("Discount cannot exceed 100%")
        }
    }
    
    /**
     * Checks if a return amount is within threshold.
     */
    fun checkReturn(user: AuthUser, amount: BigDecimal): ThresholdCheckResult {
        val thresholds = PermissionThresholds.forRole(user.role)
        
        return when {
            amount <= thresholds.maxReturnAmount -> ThresholdCheckResult.Allowed
            else -> ThresholdCheckResult.RequiresApproval(
                reason = "Return amount exceeds \$${thresholds.maxReturnAmount} limit",
                requestedAmount = amount,
                maxAllowed = thresholds.maxReturnAmount
            )
        }
    }
    
    /**
     * Checks if a void amount is within threshold.
     */
    fun checkVoid(user: AuthUser, amount: BigDecimal): ThresholdCheckResult {
        val thresholds = PermissionThresholds.forRole(user.role)
        
        if (thresholds.maxVoidAmount == BigDecimal.ZERO) {
            return ThresholdCheckResult.RequiresApproval(
                reason = "Void transaction requires manager approval",
                requestedAmount = amount,
                maxAllowed = BigDecimal.ZERO
            )
        }
        
        return when {
            amount <= thresholds.maxVoidAmount -> ThresholdCheckResult.Allowed
            else -> ThresholdCheckResult.RequiresApproval(
                reason = "Void amount exceeds \$${thresholds.maxVoidAmount} limit",
                requestedAmount = amount,
                maxAllowed = thresholds.maxVoidAmount
            )
        }
    }
    
    /**
     * Checks if floor price override is allowed.
     */
    fun checkFloorPriceOverride(user: AuthUser): ThresholdCheckResult {
        val thresholds = PermissionThresholds.forRole(user.role)
        
        return if (thresholds.allowFloorPriceOverride) {
            ThresholdCheckResult.Allowed
        } else {
            ThresholdCheckResult.RequiresApproval(
                reason = "Floor price override requires manager approval",
                requestedAmount = BigDecimal.ZERO,
                maxAllowed = BigDecimal.ZERO
            )
        }
    }
    
    /**
     * Checks if a cash pickup amount is within threshold.
     */
    fun checkCashPickup(user: AuthUser, amount: BigDecimal): ThresholdCheckResult {
        val thresholds = PermissionThresholds.forRole(user.role)
        
        if (thresholds.maxCashPickupAmount == BigDecimal.ZERO) {
            return ThresholdCheckResult.RequiresApproval(
                reason = "Cash pickup requires manager approval",
                requestedAmount = amount,
                maxAllowed = BigDecimal.ZERO
            )
        }
        
        return when {
            amount <= thresholds.maxCashPickupAmount -> ThresholdCheckResult.Allowed
            else -> ThresholdCheckResult.RequiresApproval(
                reason = "Cash pickup exceeds \$${thresholds.maxCashPickupAmount} limit",
                requestedAmount = amount,
                maxAllowed = thresholds.maxCashPickupAmount
            )
        }
    }
    
    /**
     * Checks if a lottery payout amount is within threshold.
     */
    fun checkLotteryPayout(user: AuthUser, amount: BigDecimal): ThresholdCheckResult {
        val thresholds = PermissionThresholds.forRole(user.role)
        
        return when {
            amount <= thresholds.maxLotteryPayoutAmount -> ThresholdCheckResult.Allowed
            else -> ThresholdCheckResult.RequiresApproval(
                reason = "Lottery payout exceeds \$${thresholds.maxLotteryPayoutAmount} limit",
                requestedAmount = amount,
                maxAllowed = thresholds.maxLotteryPayoutAmount
            )
        }
    }
}

