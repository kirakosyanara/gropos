package com.unisight.gropos.core.security

/**
 * Permission string constants following the format: {App}.{Category}.{Action}
 * 
 * Per REMEDIATION_CHECKLIST: Permission Strings format {App}.{Category}.{Action}
 * Per ROLES_AND_PERMISSIONS.md: Standard permission string format for the POS system.
 * 
 * Pattern:
 * - Base permission: "GroPOS.Category.Action" (grants direct access)
 * - Request permission: "GroPOS.Category.Action.Request" (can request approval)
 * - Self-approval: "GroPOS.Category.Action.Self Approval" (manager can self-approve)
 */
object PermissionStrings {
    
    // App prefix
    private const val APP = "GroPOS"
    
    // ========================================================================
    // Transaction Permissions
    // ========================================================================
    object Transactions {
        private const val CATEGORY = "$APP.Transactions"
        
        /** Void a transaction */
        const val VOID = "$CATEGORY.Void"
        const val VOID_REQUEST = "$VOID.Request"
        const val VOID_SELF_APPROVAL = "$VOID.Self Approval"
        
        /** Price override on an item */
        const val PRICE_OVERRIDE = "$CATEGORY.Price Override"
        const val PRICE_OVERRIDE_REQUEST = "$PRICE_OVERRIDE.Request"
        const val PRICE_OVERRIDE_SELF_APPROVAL = "$PRICE_OVERRIDE.Self Approval"
        
        /** Discounts */
        object Discounts {
            private const val DISCOUNT_CATEGORY = "$CATEGORY.Discounts"
            
            /** Line item discount */
            const val LINE_ITEM = "$DISCOUNT_CATEGORY.Items"
            const val LINE_ITEM_REQUEST = "$LINE_ITEM.Request"
            const val LINE_ITEM_SELF_APPROVAL = "$LINE_ITEM.Self Approval"
            
            /** Transaction/total discount */
            const val TRANSACTION = "$DISCOUNT_CATEGORY.Total"
            const val TRANSACTION_REQUEST = "$TRANSACTION.Request"
            const val TRANSACTION_SELF_APPROVAL = "$TRANSACTION.Self Approval"
            
            /** Floor price override (sell below cost) */
            const val FLOOR_PRICE_OVERRIDE = "$DISCOUNT_CATEGORY.Floor Price Override"
            const val FLOOR_PRICE_OVERRIDE_REQUEST = "$FLOOR_PRICE_OVERRIDE.Request"
            const val FLOOR_PRICE_OVERRIDE_SELF_APPROVAL = "$FLOOR_PRICE_OVERRIDE.Self Approval"
        }
    }
    
    // ========================================================================
    // Returns Permissions
    // ========================================================================
    object Returns {
        private const val CATEGORY = "$APP.Returns"
        
        /** Process a return */
        const val PROCESS = CATEGORY
        const val PROCESS_REQUEST = "$CATEGORY.Request"
        const val PROCESS_SELF_APPROVAL = "$CATEGORY.Self Approval"
        
        /** Return without receipt */
        const val NO_RECEIPT = "$CATEGORY.No Receipt"
        const val NO_RECEIPT_REQUEST = "$NO_RECEIPT.Request"
        const val NO_RECEIPT_SELF_APPROVAL = "$NO_RECEIPT.Self Approval"
    }
    
    // ========================================================================
    // Cash Operations Permissions
    // ========================================================================
    object Cash {
        private const val CATEGORY = "$APP.Cash"
        
        /** Add cash to drawer */
        const val ADD = "$CATEGORY.Add"
        const val ADD_REQUEST = "$ADD.Request"
        const val ADD_SELF_APPROVAL = "$ADD.Self Approval"
        
        /** Cash pickup from drawer */
        const val PICKUP = "$APP.Cash Pickup."
        const val PICKUP_REQUEST = "$PICKUP.Request"
        const val PICKUP_SELF_APPROVAL = "$PICKUP.Self Approval"
        
        /** Safe drop */
        const val SAFE_DROP = "$CATEGORY.Safe Drop"
        const val SAFE_DROP_REQUEST = "$SAFE_DROP.Request"
    }
    
    // ========================================================================
    // Lottery Permissions
    // ========================================================================
    object Lottery {
        private const val CATEGORY = "$APP.Lottery"
        
        /** Lottery sales */
        const val SALE = "$CATEGORY.Sale"
        
        /** Lottery payouts by tier */
        object Payout {
            private const val PAYOUT_CATEGORY = "$CATEGORY.Payout"
            
            /** Tier 1: Small payouts (e.g., up to $100) */
            const val TIER_1 = "$PAYOUT_CATEGORY.Tier1"
            
            /** Tier 2: Medium payouts (e.g., $100-$599) */
            const val TIER_2 = "$PAYOUT_CATEGORY.Tier2"
            
            /** Tier 3: Large payouts (e.g., $600+) */
            const val TIER_3 = "$PAYOUT_CATEGORY.Tier3"
            const val TIER_3_REQUEST = "$TIER_3.Request"
            const val TIER_3_SELF_APPROVAL = "$TIER_3.Self Approval"
        }
    }
    
    // ========================================================================
    // Vendor Permissions
    // ========================================================================
    object Vendor {
        private const val CATEGORY = "$APP.VendorPayout"
        
        /** Vendor payout */
        const val PAYOUT = CATEGORY
        const val PAYOUT_REQUEST = "$PAYOUT.Request"
        const val PAYOUT_SELF_APPROVAL = "$PAYOUT.Self Approval"
    }
    
    // ========================================================================
    // Store Operations Permissions
    // ========================================================================
    object Store {
        private const val CATEGORY = "$APP.Store"
        
        /** Force sign out */
        const val FORCE_SIGN_OUT = "$CATEGORY.Force Sign Out"
        const val FORCE_SIGN_OUT_REQUEST = "$FORCE_SIGN_OUT.Request"
        const val FORCE_SIGN_OUT_SELF_APPROVAL = "$FORCE_SIGN_OUT.Self Approval"
        
        /** Open register */
        const val OPEN_REGISTER = "$CATEGORY.Open Register"
        
        /** Close register */
        const val CLOSE_REGISTER = "$CATEGORY.Close Register"
    }
    
    // ========================================================================
    // Settings Permissions
    // ========================================================================
    object Settings {
        private const val CATEGORY = "$APP.Settings"
        
        /** Access hardware settings */
        const val HARDWARE = "$CATEGORY.Hardware"
        
        /** Access store settings */
        const val STORE = "$CATEGORY.Store"
        
        /** Access user management */
        const val USERS = "$CATEGORY.Users"
    }
    
    // ========================================================================
    // Helper Functions
    // ========================================================================
    
    /**
     * Generates a request permission from a base permission.
     */
    fun toRequest(basePermission: String): String = "$basePermission.Request"
    
    /**
     * Generates a self-approval permission from a base permission.
     */
    fun toSelfApproval(basePermission: String): String = "$basePermission.Self Approval"
    
    /**
     * Parses a permission string into its components.
     */
    fun parse(permission: String): PermissionComponents {
        val parts = permission.split(".")
        return PermissionComponents(
            app = parts.getOrNull(0) ?: "",
            category = parts.getOrNull(1) ?: "",
            action = parts.getOrNull(2) ?: "",
            modifier = parts.getOrNull(3)
        )
    }
}

/**
 * Components of a parsed permission string.
 */
data class PermissionComponents(
    val app: String,
    val category: String,
    val action: String,
    val modifier: String? = null
) {
    val isRequest: Boolean get() = modifier == "Request"
    val isSelfApproval: Boolean get() = modifier == "Self Approval"
    val basePermission: String get() = "$app.$category.$action"
}

