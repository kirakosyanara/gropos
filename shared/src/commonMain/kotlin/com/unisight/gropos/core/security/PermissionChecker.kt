package com.unisight.gropos.core.security

import com.unisight.gropos.features.auth.domain.model.AuthUser
import java.math.BigDecimal

/**
 * Utility for checking specific permissions with business logic.
 * 
 * Per REMEDIATION_CHECKLIST:
 * - Price Override Permission Check
 * - Discount Permission Check
 * 
 * Combines PermissionManager with ThresholdChecker for comprehensive authorization.
 */
object PermissionChecker {
    
    // ========================================================================
    // Discount Permission Checks
    // ========================================================================
    
    /**
     * Checks if user can apply a line item discount.
     * 
     * Per REMEDIATION_CHECKLIST: Discount Permission Check.
     * 
     * @param user The current user
     * @param discountPercent The discount percentage to apply
     * @return Permission result with approval requirements
     */
    fun checkLineDiscountPermission(
        user: AuthUser,
        discountPercent: Int
    ): DiscountPermissionResult {
        // First check the base permission
        val permissionResult = PermissionManager.checkPermission(
            user,
            RequestAction.LINE_DISCOUNT
        )
        
        // If denied at permission level, return denied
        if (permissionResult == PermissionCheckResult.DENIED) {
            return DiscountPermissionResult.Denied("You do not have permission to apply line discounts")
        }
        
        // Check against thresholds
        val thresholdResult = ThresholdChecker.checkLineDiscount(user, discountPercent)
        
        return when (thresholdResult) {
            is ThresholdCheckResult.Allowed -> {
                when (permissionResult) {
                    PermissionCheckResult.GRANTED,
                    PermissionCheckResult.SELF_APPROVAL_ALLOWED -> DiscountPermissionResult.Allowed
                    PermissionCheckResult.REQUIRES_APPROVAL -> DiscountPermissionResult.RequiresApproval(
                        "Discount requires manager approval"
                    )
                    PermissionCheckResult.DENIED -> DiscountPermissionResult.Denied("Permission denied")
                }
            }
            is ThresholdCheckResult.RequiresApproval -> {
                DiscountPermissionResult.RequiresApproval(thresholdResult.reason)
            }
            is ThresholdCheckResult.Denied -> {
                DiscountPermissionResult.Denied(thresholdResult.reason)
            }
        }
    }
    
    /**
     * Checks if user can apply a transaction discount.
     * 
     * Per REMEDIATION_CHECKLIST: Discount Permission Check.
     * 
     * @param user The current user
     * @param discountPercent The discount percentage to apply
     * @return Permission result with approval requirements
     */
    fun checkTransactionDiscountPermission(
        user: AuthUser,
        discountPercent: Int
    ): DiscountPermissionResult {
        // First check the base permission
        val permissionResult = PermissionManager.checkPermission(
            user,
            RequestAction.TRANSACTION_DISCOUNT
        )
        
        // If denied at permission level, return denied
        if (permissionResult == PermissionCheckResult.DENIED) {
            return DiscountPermissionResult.Denied("You do not have permission to apply transaction discounts")
        }
        
        // Check against thresholds
        val thresholdResult = ThresholdChecker.checkTransactionDiscount(user, discountPercent)
        
        return when (thresholdResult) {
            is ThresholdCheckResult.Allowed -> {
                when (permissionResult) {
                    PermissionCheckResult.GRANTED,
                    PermissionCheckResult.SELF_APPROVAL_ALLOWED -> DiscountPermissionResult.Allowed
                    PermissionCheckResult.REQUIRES_APPROVAL -> DiscountPermissionResult.RequiresApproval(
                        "Transaction discount requires manager approval"
                    )
                    PermissionCheckResult.DENIED -> DiscountPermissionResult.Denied("Permission denied")
                }
            }
            is ThresholdCheckResult.RequiresApproval -> {
                DiscountPermissionResult.RequiresApproval(thresholdResult.reason)
            }
            is ThresholdCheckResult.Denied -> {
                DiscountPermissionResult.Denied(thresholdResult.reason)
            }
        }
    }
    
    // ========================================================================
    // Price Override Permission Checks
    // ========================================================================
    
    /**
     * Checks if user can override the price of an item.
     * 
     * Per REMEDIATION_CHECKLIST: Price Override Permission Check.
     * 
     * @param user The current user
     * @param originalPrice The original price
     * @param newPrice The new price to set
     * @param floorPrice The minimum allowed price (floor price)
     * @return Permission result with approval requirements
     */
    fun checkPriceOverridePermission(
        user: AuthUser,
        originalPrice: BigDecimal,
        newPrice: BigDecimal,
        floorPrice: BigDecimal?
    ): PriceOverridePermissionResult {
        // Check if this is a floor price override
        val isBelowFloor = floorPrice != null && newPrice < floorPrice
        
        if (isBelowFloor) {
            return checkFloorPriceOverridePermission(user, newPrice, floorPrice!!)
        }
        
        // Regular price override
        val permissionResult = PermissionManager.checkPermission(
            user,
            RequestAction.PRICE_OVERRIDE
        )
        
        return when (permissionResult) {
            PermissionCheckResult.GRANTED,
            PermissionCheckResult.SELF_APPROVAL_ALLOWED -> PriceOverridePermissionResult.Allowed
            PermissionCheckResult.REQUIRES_APPROVAL -> PriceOverridePermissionResult.RequiresApproval(
                "Price override requires manager approval"
            )
            PermissionCheckResult.DENIED -> PriceOverridePermissionResult.Denied(
                "You do not have permission to override prices"
            )
        }
    }
    
    /**
     * Checks if user can override a price below the floor price.
     * 
     * Per REMEDIATION_CHECKLIST: Price Override Permission Check (floor price).
     * 
     * @param user The current user
     * @param newPrice The new price to set
     * @param floorPrice The minimum allowed price (floor price)
     * @return Permission result with approval requirements
     */
    fun checkFloorPriceOverridePermission(
        user: AuthUser,
        newPrice: BigDecimal,
        floorPrice: BigDecimal
    ): PriceOverridePermissionResult {
        val permissionResult = PermissionManager.checkPermission(
            user,
            RequestAction.FLOOR_PRICE_OVERRIDE
        )
        
        // Also check threshold-based permission
        val thresholdResult = ThresholdChecker.checkFloorPriceOverride(user)
        
        return when {
            thresholdResult is ThresholdCheckResult.Denied -> {
                PriceOverridePermissionResult.Denied(
                    "Floor price override is not allowed for your role"
                )
            }
            permissionResult == PermissionCheckResult.DENIED -> {
                PriceOverridePermissionResult.Denied(
                    "You do not have permission to sell below floor price"
                )
            }
            thresholdResult is ThresholdCheckResult.RequiresApproval -> {
                PriceOverridePermissionResult.RequiresApproval(
                    "Selling below floor price (\$$floorPrice) requires manager approval"
                )
            }
            permissionResult == PermissionCheckResult.REQUIRES_APPROVAL -> {
                PriceOverridePermissionResult.RequiresApproval(
                    "Floor price override requires manager approval"
                )
            }
            else -> PriceOverridePermissionResult.Allowed
        }
    }
    
    // ========================================================================
    // Return Permission Checks
    // ========================================================================
    
    /**
     * Checks if user can process a return.
     * 
     * @param user The current user
     * @param returnAmount The total return amount
     * @return Permission result with approval requirements
     */
    fun checkReturnPermission(
        user: AuthUser,
        returnAmount: BigDecimal
    ): ReturnPermissionResult {
        val permissionResult = PermissionManager.checkPermission(
            user,
            RequestAction.RETURN_ITEM
        )
        
        val thresholdResult = ThresholdChecker.checkReturn(user, returnAmount)
        
        return when {
            permissionResult == PermissionCheckResult.DENIED -> {
                ReturnPermissionResult.Denied("You do not have permission to process returns")
            }
            thresholdResult is ThresholdCheckResult.Denied -> {
                ReturnPermissionResult.Denied(thresholdResult.reason)
            }
            thresholdResult is ThresholdCheckResult.RequiresApproval -> {
                ReturnPermissionResult.RequiresApproval(thresholdResult.reason)
            }
            permissionResult == PermissionCheckResult.REQUIRES_APPROVAL -> {
                ReturnPermissionResult.RequiresApproval("Return requires manager approval")
            }
            else -> ReturnPermissionResult.Allowed
        }
    }
    
    // ========================================================================
    // Void Permission Checks
    // ========================================================================
    
    /**
     * Checks if user can void a transaction.
     * 
     * @param user The current user
     * @param voidAmount The transaction amount being voided
     * @return Permission result with approval requirements
     */
    fun checkVoidPermission(
        user: AuthUser,
        voidAmount: BigDecimal
    ): VoidPermissionResult {
        val permissionResult = PermissionManager.checkPermission(
            user,
            RequestAction.VOID_TRANSACTION
        )
        
        val thresholdResult = ThresholdChecker.checkVoid(user, voidAmount)
        
        return when {
            permissionResult == PermissionCheckResult.DENIED -> {
                VoidPermissionResult.Denied("You do not have permission to void transactions")
            }
            thresholdResult is ThresholdCheckResult.Denied -> {
                VoidPermissionResult.Denied(thresholdResult.reason)
            }
            thresholdResult is ThresholdCheckResult.RequiresApproval -> {
                VoidPermissionResult.RequiresApproval(thresholdResult.reason)
            }
            permissionResult == PermissionCheckResult.REQUIRES_APPROVAL -> {
                VoidPermissionResult.RequiresApproval("Void requires manager approval")
            }
            else -> VoidPermissionResult.Allowed
        }
    }
}

// ========================================================================
// Result Types
// ========================================================================

/**
 * Result of a discount permission check.
 */
sealed class DiscountPermissionResult {
    data object Allowed : DiscountPermissionResult()
    data class RequiresApproval(val reason: String) : DiscountPermissionResult()
    data class Denied(val reason: String) : DiscountPermissionResult()
}

/**
 * Result of a price override permission check.
 */
sealed class PriceOverridePermissionResult {
    data object Allowed : PriceOverridePermissionResult()
    data class RequiresApproval(val reason: String) : PriceOverridePermissionResult()
    data class Denied(val reason: String) : PriceOverridePermissionResult()
}

/**
 * Result of a return permission check.
 */
sealed class ReturnPermissionResult {
    data object Allowed : ReturnPermissionResult()
    data class RequiresApproval(val reason: String) : ReturnPermissionResult()
    data class Denied(val reason: String) : ReturnPermissionResult()
}

/**
 * Result of a void permission check.
 */
sealed class VoidPermissionResult {
    data object Allowed : VoidPermissionResult()
    data class RequiresApproval(val reason: String) : VoidPermissionResult()
    data class Denied(val reason: String) : VoidPermissionResult()
}

