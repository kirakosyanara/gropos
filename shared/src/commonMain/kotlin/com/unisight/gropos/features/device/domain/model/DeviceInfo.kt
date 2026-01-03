package com.unisight.gropos.features.device.domain.model

/**
 * Device information model for registration and identification.
 * 
 * Per DEVICE_REGISTRATION.md:
 * - PosSystemViewModel structure with apiKey, branchName, stationId
 * - Stored in CouchbaseLite after successful registration
 * 
 * Per DATA_MODELS.md:
 * - Device is registered to a specific branch
 * - API key authenticates all subsequent API requests
 */
data class DeviceInfo(
    val stationId: String,       // Unique device GUID
    val apiKey: String,          // API key received from registration
    val branchName: String,      // Display name of the branch
    val branchId: Int = -1,      // Branch entity ID
    val environment: String = "PRODUCTION", // Environment: PRODUCTION, STAGING, DEVELOPMENT
    val registeredAt: String = "" // ISO-8601 timestamp of registration
)

/**
 * Registration state machine per DEVICE_REGISTRATION.md Section 3.
 * 
 * **H1 FIX:** Added LOADING and TIMEOUT states per documentation.
 */
enum class RegistrationState {
    /**
     * Initial state, checking local database for existing registration.
     * H1 FIX: Added per DEVICE_REGISTRATION.md
     */
    LOADING,
    
    /** No API key found, need to show QR/pairing code */
    UNREGISTERED,
    
    /** QR/pairing code displayed, waiting for admin to scan/enter */
    PENDING,
    
    /** Admin has scanned/entered code, assigning branch in admin portal */
    IN_PROGRESS,
    
    /** API key received and saved, ready for login */
    REGISTERED,
    
    /**
     * QR code expired (default: 10 minutes, extended: 60 minutes for IN_PROGRESS).
     * H1 FIX: Added per DEVICE_REGISTRATION.md
     */
    TIMEOUT,
    
    /** Registration failed due to error (network, server, etc.) */
    ERROR
}

/**
 * Response from QR code registration API.
 * Per DEVICE_REGISTRATION.md API Endpoints.
 */
data class QrRegistrationResponse(
    val url: String?,           // Admin portal URL for activation
    val qrCodeImage: String?,   // Base64-encoded PNG image
    val accessToken: String?,   // Temporary access token for status polling
    val assignedGuid: String?   // Device GUID for status checks
)

/**
 * Response from device status polling API.
 * Per DEVICE_REGISTRATION.md API Endpoints.
 */
data class DeviceStatusResponse(
    val deviceStatus: String?,  // "Pending", "In-Progress", "Registered"
    val apiKey: String?,        // Provided when status is "Registered"
    val branchId: Int?,         // Branch ID
    val branch: String?         // Branch display name
)

