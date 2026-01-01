package com.unisight.gropos.features.auth.domain.model

/**
 * Defines the access level for authenticated users.
 * 
 * Role hierarchy determines which operations a user can perform:
 * - CASHIER: Basic transaction operations
 * - MANAGER: Overrides, voids, returns, reports
 * - ADMIN: Full system access including settings
 */
enum class UserRole {
    CASHIER,
    MANAGER,
    ADMIN
}

