package com.unisight.gropos.features.device.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.core.storage.SecureStorage
import com.unisight.gropos.features.device.data.dto.DeviceDomainMapper.toDomain
import com.unisight.gropos.features.device.data.dto.DeviceStatusResponseDto
import com.unisight.gropos.features.device.data.dto.QrRegistrationRequest
import com.unisight.gropos.features.device.data.dto.QrRegistrationResponseDto
import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import io.ktor.client.request.get
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
 * - POST /device/heartbeat - Device heartbeat (separate from registration)
 */
class RemoteDeviceRepository(
    private val apiClient: ApiClient,
    private val secureStorage: SecureStorage,
    private val platformInfo: PlatformInfo = DefaultPlatformInfo
) : DeviceRepository {
    
    companion object {
        private const val ENDPOINT_QR_REGISTRATION = "/device-registration/qr-registration"
        private const val ENDPOINT_DEVICE_STATUS = "/device-registration/device-status/{deviceGuid}"
    }
    
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
     * **API:** POST /device-registration/qr-registration
     * **Response:** QrRegistrationResponseDto with URL and device GUID
     */
    override suspend fun requestQrCode(): Result<QrRegistrationResponse> {
        val request = QrRegistrationRequest(
            deviceName = platformInfo.getDeviceName(),
            platform = platformInfo.getPlatform()
        )
        
        return apiClient.deviceRequest<QrRegistrationResponseDto> {
            post(ENDPOINT_QR_REGISTRATION) {
                setBody(request)
            }
        }.map { it.toDomain() }
    }
    
    /**
     * Polls for device registration status.
     * 
     * **API:** GET /device-registration/device-status/{deviceGuid}
     * **Response:** DeviceStatusResponseDto with status and optionally API key
     * 
     * When status is "Registered", the caller should call registerDevice()
     * with the returned credentials.
     */
    override suspend fun checkRegistrationStatus(deviceGuid: String): Result<DeviceStatusResponse> {
        val endpoint = ENDPOINT_DEVICE_STATUS.replace("{deviceGuid}", deviceGuid)
        
        return apiClient.deviceRequest<DeviceStatusResponseDto> {
            get(endpoint)
        }.map { it.toDomain() }
    }
}

/**
 * Exception for device registration failures.
 */
class DeviceRegistrationException(message: String) : Exception(message)

/**
 * Platform info provider for device registration.
 */
interface PlatformInfo {
    fun getDeviceName(): String
    fun getPlatform(): String
}

/**
 * Default platform info (can be overridden per platform).
 */
object DefaultPlatformInfo : PlatformInfo {
    override fun getDeviceName(): String = "GroPOS Device"
    override fun getPlatform(): String = "DESKTOP"
}

