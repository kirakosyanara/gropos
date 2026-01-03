package com.unisight.gropos.features.pricing.domain.model

import java.math.BigDecimal

/**
 * Represents a customer group for special pricing.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroup collection.
 * Used for EBT/SNAP, employee discounts, and other group-based pricing.
 * 
 * @property id Unique group identifier
 * @property name Display name of the group (e.g., "Employee", "Senior Discount")
 * @property statusId Current status (Active/Inactive)
 * @property isActive Computed property for easy status checking
 * @property createdDate Record creation timestamp
 * @property updatedDate Last update timestamp
 */
data class CustomerGroup(
    val id: Int,
    val name: String,
    val statusId: String = "Active",
    val createdDate: String? = null,
    val updatedDate: String? = null
) {
    /**
     * Whether this group is currently active and should be applied.
     */
    val isActive: Boolean
        get() = statusId == "Active"
}

/**
 * Represents department-level pricing for a customer group.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroupDepartment collection.
 * Allows a discount percentage to be applied to all products in a department.
 * 
 * @property id Unique record identifier
 * @property customerGroupId FK to CustomerGroup
 * @property departmentId FK to Department/Category
 * @property departmentName Department display name (denormalized)
 * @property discountPercent Discount percentage as decimal (0.10 = 10%)
 * @property createdDate Record creation timestamp
 * @property updatedDate Last update timestamp
 */
data class CustomerGroupDepartment(
    val id: Int,
    val customerGroupId: Int,
    val departmentId: Int,
    val departmentName: String? = null,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val createdDate: String? = null,
    val updatedDate: String? = null
)

/**
 * Represents item-level pricing for a customer group.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroupItem collection.
 * Allows specific products to have fixed prices or discounts for a group.
 * 
 * @property id Unique record identifier
 * @property customerGroupId FK to CustomerGroup
 * @property branchProductId FK to Product (branch-specific)
 * @property productName Product display name (denormalized)
 * @property specialPrice Fixed special price for this group (null = use discount)
 * @property discountPercent Discount percentage as decimal (0.10 = 10%)
 * @property createdDate Record creation timestamp
 * @property updatedDate Last update timestamp
 */
data class CustomerGroupItem(
    val id: Int,
    val customerGroupId: Int,
    val branchProductId: Int,
    val productName: String? = null,
    val specialPrice: BigDecimal? = null,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val createdDate: String? = null,
    val updatedDate: String? = null
) {
    /**
     * Whether this item has a fixed special price.
     */
    val hasSpecialPrice: Boolean
        get() = specialPrice != null && specialPrice > BigDecimal.ZERO
    
    /**
     * Whether this item has a discount percentage.
     */
    val hasDiscount: Boolean
        get() = discountPercent > BigDecimal.ZERO
}

