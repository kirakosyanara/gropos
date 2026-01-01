package com.unisight.gropos.features.auth.domain.model

/**
 * Represents an authenticated user in the POS system.
 * 
 * This is a pure domain model with no framework dependencies.
 * Used across all platform targets (Desktop/Android).
 *
 * @property id Unique identifier for the user
 * @property username Display name used for receipts and logs
 * @property role Access level determining permitted operations
 */
data class AuthUser(
    val id: String,
    val username: String,
    val role: UserRole
)

