package com.unisight.gropos.core.auth

import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Auth request/response DTOs for API communication.
 * 
 * **Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:**
 * - POST /api/Employee/Login - Cashier authentication with till assignment
 * - POST /api/Employee/VerifyPassword - PIN verification on lock screen
 * - POST /api/Employee/LockDevice - Report lock/unlock events
 * - POST /api/Employee/Logout - End session
 * - POST /api/Employee/LogoutWithEndOfShift - End-of-shift logout
 * 
 * **Per zero-trust-security.mdc:**
 * - PINs are only transmitted over HTTPS
 * - Tokens are stored securely after receipt
 */

// ========================================================================
// Enums
// ========================================================================

/**
 * Device event types for lock/unlock tracking.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Locked(4): Manual lock (keyboard shortcut or lock button)
 * - Unlocked(5): Successful PIN verification
 * - AutoLocked(6): Inactivity timeout
 */
@Serializable
enum class DeviceEventType(val value: Int) {
    @SerialName("0") SignedIn(0),
    @SerialName("1") SignedOut(1),
    @SerialName("2") ClockedIn(2),
    @SerialName("3") ClockedOut(3),
    @SerialName("4") Locked(4),
    @SerialName("5") Unlocked(5),
    @SerialName("6") AutoLocked(6)
}

// ========================================================================
// Request DTOs
// ========================================================================

/**
 * Login request with PIN and branch ID.
 * 
 * Per AUTHENTICATION.md: PIN-based authentication (legacy).
 */
@Serializable
data class LoginRequest(
    val pin: String,
    @SerialName("branch")
    val branchId: Int
)

/**
 * Cashier login request with till assignment.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - POST /api/Employee/Login
 * - locationAccountId (till ID) is REQUIRED
 */
@Serializable
data class CashierLoginRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val authenticationKey: String? = null,
    val locationAccountId: Int,  // Till ID (REQUIRED)
    val branchId: Int,
    val deviceId: Int
)

/**
 * Verify password request for lock screen PIN verification.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - POST /api/Employee/VerifyPassword
 * - Used to unlock a locked station
 * - Does NOT refresh tokens (session remains active)
 */
@Serializable
data class VerifyPasswordRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val branchId: Int? = null,
    val deviceId: Int? = null
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
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - POST /api/Employee/LockDevice
 * - Reports lock/unlock events to backend
 */
@Serializable
data class DeviceLockRequest(
    val lockType: DeviceEventType
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
 * Response from lock device endpoint.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Returned from POST /api/Employee/LockDevice
 */
@Serializable
data class DeviceEventResponse(
    val success: Boolean = true,
    val eventId: Long? = null,
    val message: String? = null
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

