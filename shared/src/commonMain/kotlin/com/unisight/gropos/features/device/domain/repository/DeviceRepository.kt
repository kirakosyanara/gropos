package com.unisight.gropos.features.device.domain.repository

import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse

/**
 * Repository interface for device registration and management.
 * 
 * Per DEVICE_REGISTRATION.md:
 * - Manages device registration state
 * - Persists API key and branch info locally
 * - Handles QR code generation and status polling
 * 
 * Per project-structure.mdc:
 * - Interface defined in Domain layer
 * - Implementations in Data layer (FakeDeviceRepository, CouchbaseDeviceRepository)
 */
interface DeviceRepository {
    
    /**
     * Check if this device is registered.
     * 
     * Per DEVICE_REGISTRATION.md:
     * - Returns true if stationId and apiKey exist in local storage
     * - Used on app launch to determine if registration flow is needed
     */
    suspend fun isRegistered(): Boolean
    
    /**
     * Get the current device info if registered.
     * 
     * @return DeviceInfo if registered, null otherwise
     */
    suspend fun getDeviceInfo(): DeviceInfo?
    
    /**
     * Register the device with an activation token.
     * 
     * Per DEVICE_REGISTRATION.md:
     * - Called when polling detects "Registered" status
     * - Saves API key and branch info to local storage
     * 
     * @param deviceInfo The device registration data to save
     * @return Result.success if saved, Result.failure on error
     */
    suspend fun registerDevice(deviceInfo: DeviceInfo): Result<Unit>
    
    /**
     * Clear device registration (for environment change or reset).
     * 
     * Per SCREEN_LAYOUTS.md - Hidden Settings Menu:
     * - Used when wiping database and changing environment
     */
    suspend fun clearRegistration(): Result<Unit>
    
    /**
     * Generate a pairing code for device activation.
     * 
     * Per DEVICE_REGISTRATION.md:
     * - Random 8-character alphanumeric code
     * - Format: XXXX-XXXX (4 chars, hyphen, 4 chars)
     * - Used for manual entry in admin portal
     */
    fun generatePairingCode(): String
    
    // ========================================================================
    // API Methods (Mock implementations for P2, real API later)
    // ========================================================================
    
    /**
     * Request a QR code for registration.
     * 
     * Per DEVICE_REGISTRATION.md:
     * POST /device-registration/qr-registration
     * 
     * @return QR code data with URL, image, and GUID
     */
    suspend fun requestQrCode(): Result<QrRegistrationResponse>
    
    /**
     * Poll for device registration status.
     * 
     * Per DEVICE_REGISTRATION.md:
     * GET /device-registration/device-status/{deviceGuid}
     * 
     * @param deviceGuid The device GUID from QR registration
     * @return Current registration status
     */
    suspend fun checkRegistrationStatus(deviceGuid: String): Result<DeviceStatusResponse>
}

