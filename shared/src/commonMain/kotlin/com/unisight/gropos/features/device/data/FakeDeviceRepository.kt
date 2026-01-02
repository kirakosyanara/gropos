package com.unisight.gropos.features.device.data

import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import kotlin.random.Random

/**
 * Fake implementation of DeviceRepository for development/testing.
 * 
 * Per DEVICE_REGISTRATION.md:
 * - Simulates device registration flow
 * - Stores registration in memory (not persistent)
 * 
 * For P2:
 * - This is a walking skeleton with in-memory storage
 * - Real persistence will be added via Settings/Preferences or CouchbaseLite
 */
class FakeDeviceRepository : DeviceRepository {
    
    // In-memory storage for development
    private var storedDeviceInfo: DeviceInfo? = null
    
    // Simulate polling states
    private var pollCount = 0
    
    override suspend fun isRegistered(): Boolean {
        return storedDeviceInfo?.apiKey?.isNotEmpty() == true
    }
    
    override suspend fun getDeviceInfo(): DeviceInfo? {
        return storedDeviceInfo
    }
    
    override suspend fun registerDevice(deviceInfo: DeviceInfo): Result<Unit> {
        storedDeviceInfo = deviceInfo
        return Result.success(Unit)
    }
    
    override suspend fun clearRegistration(): Result<Unit> {
        storedDeviceInfo = null
        pollCount = 0
        return Result.success(Unit)
    }
    
    override fun generatePairingCode(): String {
        // Generate random 8-character alphanumeric code
        // Format: XXXX-XXXX
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Omit confusing chars like 0/O, 1/I
        val part1 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "$part1-$part2"
    }
    
    override suspend fun requestQrCode(): Result<QrRegistrationResponse> {
        // Simulate API response
        val pairingCode = generatePairingCode()
        val guid = generateRandomGuid()
        
        return Result.success(
            QrRegistrationResponse(
                url = "https://admin.gropos.com/register/$pairingCode",
                qrCodeImage = null, // Would be Base64 PNG in real implementation
                accessToken = "temp_token_${System.currentTimeMillis()}",
                assignedGuid = guid
            )
        )
    }
    
    override suspend fun checkRegistrationStatus(deviceGuid: String): Result<DeviceStatusResponse> {
        // Simulate polling behavior:
        // - First few calls return "Pending"
        // - After simulate activation, return "Registered" with API key
        pollCount++
        
        return when {
            storedDeviceInfo != null -> {
                // Already registered
                Result.success(
                    DeviceStatusResponse(
                        deviceStatus = "Registered",
                        apiKey = storedDeviceInfo?.apiKey,
                        branchId = storedDeviceInfo?.branchId,
                        branch = storedDeviceInfo?.branchName
                    )
                )
            }
            else -> {
                // Still pending
                Result.success(
                    DeviceStatusResponse(
                        deviceStatus = "Pending",
                        apiKey = null,
                        branchId = null,
                        branch = null
                    )
                )
            }
        }
    }
    
    /**
     * Dev tool: Simulate activation from admin portal.
     * 
     * Called by "Simulate Activation" button in RegistrationScreen.
     * Bypasses the actual cloud check for development purposes.
     */
    fun simulateActivation(
        stationId: String = generateRandomGuid(),
        apiKey: String = "dev_api_key_${System.currentTimeMillis()}",
        branchName: String = "Development Branch",
        branchId: Int = 1
    ) {
        storedDeviceInfo = DeviceInfo(
            stationId = stationId,
            apiKey = apiKey,
            branchName = branchName,
            branchId = branchId,
            environment = "DEVELOPMENT",
            registeredAt = kotlinx.datetime.Clock.System.now().toString()
        )
        pollCount = 0
    }
    
    private fun generateRandomGuid(): String {
        val chars = "0123456789abcdef"
        fun randomHex(len: Int) = (1..len).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "${randomHex(8)}-${randomHex(4)}-${randomHex(4)}-${randomHex(4)}-${randomHex(12)}"
    }
}

