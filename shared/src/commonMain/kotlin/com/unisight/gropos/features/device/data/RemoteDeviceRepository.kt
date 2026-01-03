package com.unisight.gropos.features.device.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.core.storage.SecureStorage
import com.unisight.gropos.features.device.data.dto.DeviceDomainMapper.toDomain
import com.unisight.gropos.features.device.data.dto.DeviceStatusResponseDto
import com.unisight.gropos.features.device.data.dto.DeviceTypes
import com.unisight.gropos.features.device.data.dto.QrRegistrationRequest
import com.unisight.gropos.features.device.data.dto.QrRegistrationResponseDto
import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlin.random.Random

/**
 * Remote implementation of DeviceRepository using REST API and SecureStorage.
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Handles QR code generation and status polling
 * - Saves API key and branch info to SecureStorage after registration
 * 
 * **Per zero-trust-security.mdc:**
 * - Uses SecureStorage for sensitive credentials (stationId, apiKey)
 * - Never logs or prints API keys
 * 
 * **Per API_INTEGRATION.md:**
 * - POST /device-registration/qr-registration - Get QR code
 * - GET /device-registration/device-status/{deviceGuid} - Poll status
 * - GET /device-registration/heartbeat - Device heartbeat (H2 FIX: GET, not POST)
 * 
 * **REMEDIATION FIXES:**
 * - C1: QrRegistrationRequest now uses deviceType instead of deviceName/platform
 * - C2: Status polling now includes Authorization: Bearer header
 * - H3: Stores accessToken from QR response for polling
 * - H4: Adds version: 1.0 header to all requests
 */
class RemoteDeviceRepository(
    private val apiClient: ApiClient,
    private val secureStorage: SecureStorage
) : DeviceRepository {
    
    companion object {
        private const val ENDPOINT_QR_REGISTRATION = "/device-registration/qr-registration"
        private const val ENDPOINT_DEVICE_STATUS = "/device-registration/device-status/{deviceGuid}"
        private const val ENDPOINT_HEARTBEAT = "/device-registration/heartbeat"
        private const val VERSION_HEADER = "version"
        private const val VERSION_VALUE = "1.0"
    }
    
    // H3 FIX: Temporary token storage for polling phase (not persisted)
    private var temporaryAccessToken: String? = null
    private var currentDeviceGuid: String? = null
    
    // ========================================================================
    // Local Storage Operations
    // ========================================================================
    
    /**
     * Checks if device is registered by verifying SecureStorage.
     * 
     * Per DEVICE_REGISTRATION.md: Check if stationId and apiKey exist.
     */
    override suspend fun isRegistered(): Boolean {
        return secureStorage.isRegistered()
    }
    
    /**
     * Gets device info from SecureStorage.
     */
    override suspend fun getDeviceInfo(): DeviceInfo? {
        if (!secureStorage.isRegistered()) return null
        
        return DeviceInfo(
            stationId = secureStorage.getStationId() ?: return null,
            apiKey = secureStorage.getApiKey() ?: return null,
            branchName = secureStorage.getBranchName() ?: "Unknown Branch",
            branchId = secureStorage.getBranchId() ?: -1,
            environment = secureStorage.getEnvironment() ?: "PRODUCTION"
        )
    }
    
    /**
     * Saves device registration to SecureStorage.
     * 
     * **CRITICAL:** This is called when polling detects "Registered" status.
     * Per DEVICE_REGISTRATION.md: Save API key and branch info to local storage.
     */
    override suspend fun registerDevice(deviceInfo: DeviceInfo): Result<Unit> {
        return try {
            secureStorage.saveStationId(deviceInfo.stationId)
            secureStorage.saveApiKey(deviceInfo.apiKey)
            secureStorage.saveBranchInfo(deviceInfo.branchId, deviceInfo.branchName)
            secureStorage.saveEnvironment(deviceInfo.environment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DeviceRegistrationException("Failed to save device credentials: ${e.message}"))
        }
    }
    
    /**
     * Clears device registration from SecureStorage.
     * 
     * Per SCREEN_LAYOUTS.md: Used when wiping database and changing environment.
     */
    override suspend fun clearRegistration(): Result<Unit> {
        return try {
            secureStorage.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DeviceRegistrationException("Failed to clear registration: ${e.message}"))
        }
    }
    
    /**
     * Generates a random pairing code.
     * 
     * Format: XXXX-XXXX (4 chars, hyphen, 4 chars)
     * Uses unambiguous characters (no 0/O, 1/I/l confusion).
     */
    override fun generatePairingCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val part1 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "$part1-$part2"
    }
    
    // ========================================================================
    // API Operations
    // ========================================================================
    
    /**
     * Requests a QR code for device registration.
     * 
     * **Per DEVICE_REGISTRATION.md Section 4.1:**
     * - API: POST /device-registration/qr-registration
     * - Headers: version: 1.0
     * - Request Body: { "deviceType": 0 }
     * - Response: QrRegistrationResponseDto with URL, QR image, accessToken, assignedGuid
     * 
     * **REMEDIATION FIXES:**
     * - C1: Request now uses deviceType instead of deviceName/platform
     * - H3: Stores accessToken for subsequent polling calls
     * - H4: Adds version header
     */
    override suspend fun requestQrCode(): Result<QrRegistrationResponse> {
        // C1 FIX: Use deviceType instead of deviceName/platform
        val request = QrRegistrationRequest(deviceType = DeviceTypes.GROPOS)
        
        return apiClient.deviceRequest<QrRegistrationResponseDto> {
            post(ENDPOINT_QR_REGISTRATION) {
                header(VERSION_HEADER, VERSION_VALUE)  // H4 FIX
                setBody(request)
            }
        }.map { response ->
            // H3 FIX: Store temporary token and GUID for polling
            temporaryAccessToken = response.accessToken
            currentDeviceGuid = response.assignedGuid
            response.toDomain()
        }
    }
    
    /**
     * Polls for device registration status.
     * 
     * **Per DEVICE_REGISTRATION.md Section 4.2:**
     * - API: GET /device-registration/device-status/{deviceGuid}
     * - Headers: Authorization: Bearer <accessToken>, version: 1.0
     * - Response: DeviceStatusResponseDto with status and optionally API key
     * 
     * When status is "Registered", the caller should call registerDevice()
     * with the returned credentials.
     * 
     * **REMEDIATION FIXES:**
     * - C2: Now includes Authorization: Bearer header
     * - H4: Adds version header
     */
    override suspend fun checkRegistrationStatus(deviceGuid: String): Result<DeviceStatusResponse> {
        val token = temporaryAccessToken
        if (token == null) {
            return Result.failure(DeviceRegistrationException("No access token available. Call requestQrCode() first."))
        }
        
        val endpoint = ENDPOINT_DEVICE_STATUS.replace("{deviceGuid}", deviceGuid)
        
        return apiClient.deviceRequest<DeviceStatusResponseDto> {
            get(endpoint) {
                header(VERSION_HEADER, VERSION_VALUE)  // H4 FIX
                header("Authorization", "Bearer $token")  // C2 FIX
            }
        }.map { it.toDomain() }
    }
    
    /**
     * Gets the current device GUID from the QR registration response.
     * 
     * Used by ViewModel to know which GUID to poll for.
     */
    fun getCurrentDeviceGuid(): String? = currentDeviceGuid
    
    /**
     * Clears the temporary token and GUID.
     * 
     * Called after registration completes or is cancelled.
     */
    fun clearTemporaryCredentials() {
        temporaryAccessToken = null
        currentDeviceGuid = null
    }
}

/**
 * Exception for device registration failures.
 */
class DeviceRegistrationException(message: String) : Exception(message)

