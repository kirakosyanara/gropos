package com.unisight.gropos.core.auth

import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Auth request/response DTOs for API communication.
 * 
 * **Per API_INTEGRATION.md:**
 * - POST /employee/login - Employee authentication
 * - POST /employee/refresh - Token refresh
 * - POST /employee/unlock - PIN validation
 * - POST /employee/logout - End session
 * 
 * **Per zero-trust-security.mdc:**
 * - PINs are only transmitted over HTTPS
 * - Tokens are stored securely after receipt
 */

// ========================================================================
// Request DTOs
// ========================================================================

/**
 * Login request with PIN and branch ID.
 * 
 * Per AUTHENTICATION.md: PIN-based authentication.
 */
@Serializable
data class LoginRequest(
    val pin: String,
    @SerialName("branch")
    val branchId: Int
)

/**
 * Username/password login request.
 * 
 * Alternative to PIN-based login for initial setup.
 */
@Serializable
data class CredentialLoginRequest(
    val username: String,
    val password: String,
    @SerialName("branch")
    val branchId: Int? = null
)

/**
 * Token refresh request.
 * 
 * Per API_INTEGRATION.md: Uses refresh token to get new access token.
 */
@Serializable
data class RefreshTokenRequest(
    val token: String,
    @SerialName("clientName")
    val clientName: String = "device"
)

/**
 * Unlock request with PIN.
 * 
 * Per CASHIER_OPERATIONS.md: Used to unlock a locked session.
 */
@Serializable
data class UnlockRequest(
    val pin: String
)

/**
 * Device lock request.
 * 
 * Per AUTHENTICATION.md: Report lock status to server.
 */
@Serializable
data class DeviceLockRequest(
    val lockType: String // "Locked", "AutoLocked", "Unlocked"
)

// ========================================================================
// Response DTOs
// ========================================================================

/**
 * Token response from login/refresh endpoints.
 * 
 * Per API_INTEGRATION.md: TokenViewModel structure.
 */
@Serializable
data class TokenResponseDto(
    @SerialName("accessToken")
    val accessToken: String,
    
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    
    @SerialName("expiresIn")
    val expiresIn: Long = 3600, // seconds
    
    @SerialName("tokenType")
    val tokenType: String = "Bearer"
)

/**
 * User profile response from /employee/profile endpoint.
 */
@Serializable
data class UserProfileDto(
    @SerialName("employeeId")
    val employeeId: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("role")
    val role: String,
    
    @SerialName("permissions")
    val permissions: List<String> = emptyList(),
    
    @SerialName("isManager")
    val isManager: Boolean = false,
    
    @SerialName("jobTitle")
    val jobTitle: String? = null,
    
    @SerialName("imageUrl")
    val imageUrl: String? = null
)

// ========================================================================
// Domain Mappers
// ========================================================================

/**
 * Maps UserProfileDto to AuthUser domain model.
 */
fun UserProfileDto.toDomain(): AuthUser {
    return AuthUser(
        id = employeeId,
        username = name,
        role = when (role.lowercase()) {
            "admin" -> UserRole.ADMIN
            "manager" -> UserRole.MANAGER
            "supervisor" -> UserRole.SUPERVISOR
            else -> UserRole.CASHIER
        },
        permissions = permissions,
        isManager = isManager,
        jobTitle = jobTitle,
        imageUrl = imageUrl
    )
}

/**
 * Converts TokenResponseDto and UserProfileDto to AuthResponse.
 */
fun TokenResponseDto.toAuthResponse(user: AuthUser): AuthResponse {
    return AuthResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        tokenType = tokenType,
        user = user
    )
}

