package com.unisight.gropos.features.auth.domain.model

/**
 * Defines the access level for authenticated users.
 * 
 * Per ROLES_AND_PERMISSIONS.md:
 * Role hierarchy determines which operations a user can perform:
 * - CASHIER (Level 1): Basic transaction operations
 * - SUPERVISOR (Level 3): Cashier + larger discounts, voids with approval
 * - MANAGER (Level 4): All POS operations, reports, overrides
 * - ADMIN (Level 5): Full system access including settings
 */
enum class UserRole {
    CASHIER,
    SUPERVISOR,
    MANAGER,
    ADMIN
}

