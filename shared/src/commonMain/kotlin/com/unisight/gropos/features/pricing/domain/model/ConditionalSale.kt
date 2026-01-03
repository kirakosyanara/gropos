package com.unisight.gropos.features.pricing.domain.model

/**
 * Represents a conditional sale rule (age restrictions, ID requirements).
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: ConditionalSale collection schema.
 * Used for dynamic age verification and conditional sale rules.
 * 
 * @property id Unique conditional sale identifier
 * @property name Rule display name (e.g., "Age 21 Required", "Age 18 Required")
 * @property type Condition type (AGE_21, AGE_18, ID_REQUIRED, etc.)
 * @property products List of affected product IDs
 * @property groups List of affected category/group IDs
 * @property minimumAge The minimum age required (null if not age-based)
 * @property requiresId Whether government ID must be scanned/verified
 * @property isActive Whether this rule is currently active
 */
data class ConditionalSale(
    val id: Int,
    val name: String,
    val type: ConditionalSaleType,
    val products: List<Int> = emptyList(),
    val groups: List<Int> = emptyList(),
    val minimumAge: Int? = null,
    val requiresId: Boolean = false,
    val isActive: Boolean = true
) {
    /**
     * Whether this rule requires age verification.
     */
    val isAgeRestricted: Boolean
        get() = minimumAge != null && minimumAge > 0
    
    /**
     * Checks if a product is affected by this rule.
     * 
     * @param branchProductId The product ID to check
     * @param categoryId The product's category ID (optional)
     * @return True if the product is subject to this rule
     */
    fun appliesToProduct(branchProductId: Int, categoryId: Int? = null): Boolean {
        // Direct product match
        if (products.contains(branchProductId)) return true
        
        // Group/category match
        if (categoryId != null && groups.contains(categoryId)) return true
        
        return false
    }
}

/**
 * Types of conditional sale rules.
 */
enum class ConditionalSaleType(val displayName: String) {
    AGE_21("Age 21+ Required (Alcohol)"),
    AGE_18("Age 18+ Required (Tobacco)"),
    ID_REQUIRED("ID Required"),
    TIME_RESTRICTED("Time Restricted Sale"),
    MANAGER_APPROVAL("Manager Approval Required"),
    QUANTITY_LIMITED("Quantity Limited"),
    OTHER("Other Restriction");
    
    companion object {
        /**
         * Parses a string to ConditionalSaleType.
         * 
         * @param value The string value from database
         * @return The enum value, or OTHER if not recognized
         */
        fun fromString(value: String?): ConditionalSaleType {
            return when (value?.uppercase()) {
                "AGE_21", "AGE21", "ALCOHOL" -> AGE_21
                "AGE_18", "AGE18", "TOBACCO" -> AGE_18
                "ID_REQUIRED", "ID" -> ID_REQUIRED
                "TIME_RESTRICTED", "TIME" -> TIME_RESTRICTED
                "MANAGER_APPROVAL", "MANAGER" -> MANAGER_APPROVAL
                "QUANTITY_LIMITED", "QUANTITY" -> QUANTITY_LIMITED
                else -> OTHER
            }
        }
    }
}

