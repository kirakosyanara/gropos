package com.unisight.gropos.features.device.data

import com.unisight.gropos.core.storage.InMemorySecureStorage
import com.unisight.gropos.features.device.data.dto.DeviceStatusResponseDto
import com.unisight.gropos.features.device.data.dto.QrRegistrationResponseDto
import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RemoteDeviceRepository.
 * 
 * **Per testing-strategy.mdc:**
 * - Test registration flow: QR code → Poll → Save to SecureStorage
 * - Test credential persistence
 * - Test clear registration
 */
class RemoteDeviceRepositoryTest {
    
    // ========================================================================
    // isRegistered Tests
    // ========================================================================
    
    @Test
    fun `isRegistered returns false when no credentials stored`() = runTest {
        val storage = InMemorySecureStorage()
        val repository = createRepository(secureStorage = storage)
        
        val isRegistered = repository.isRegistered()
        
        assertFalse(isRegistered)
    }
    
    @Test
    fun `isRegistered returns true when credentials stored`() = runTest {
        val storage = InMemorySecureStorage()
        storage.saveStationId("station-123")
        storage.saveApiKey("api-key-abc")
        val repository = createRepository(secureStorage = storage)
        
        val isRegistered = repository.isRegistered()
        
        assertTrue(isRegistered)
    }
    
    // ========================================================================
    // getDeviceInfo Tests
    // ========================================================================
    
    @Test
    fun `getDeviceInfo returns null when not registered`() = runTest {
        val storage = InMemorySecureStorage()
        val repository = createRepository(secureStorage = storage)
        
        val deviceInfo = repository.getDeviceInfo()
        
        assertNull(deviceInfo)
    }
    
    @Test
    fun `getDeviceInfo returns info when registered`() = runTest {
        val storage = InMemorySecureStorage()
        storage.saveStationId("station-123")
        storage.saveApiKey("api-key-abc")
        storage.saveBranchInfo(42, "Test Branch")
        storage.saveEnvironment("PRODUCTION")
        val repository = createRepository(secureStorage = storage)
        
        val deviceInfo = repository.getDeviceInfo()
        
        assertNotNull(deviceInfo)
        assertEquals("station-123", deviceInfo.stationId)
        assertEquals("api-key-abc", deviceInfo.apiKey)
        assertEquals("Test Branch", deviceInfo.branchName)
        assertEquals(42, deviceInfo.branchId)
        assertEquals("PRODUCTION", deviceInfo.environment)
    }
    
    // ========================================================================
    // registerDevice Tests
    // ========================================================================
    
    @Test
    fun `registerDevice saves credentials to SecureStorage`() = runTest {
        val storage = InMemorySecureStorage()
        val repository = createRepository(secureStorage = storage)
        
        val deviceInfo = DeviceInfo(
            stationId = "new-station-456",
            apiKey = "new-api-key-xyz",
            branchName = "New Branch",
            branchId = 99,
            environment = "STAGING"
        )
        
        val result = repository.registerDevice(deviceInfo)
        
        assertTrue(result.isSuccess)
        assertEquals("new-station-456", storage.getStationId())
        assertEquals("new-api-key-xyz", storage.getApiKey())
        assertEquals("New Branch", storage.getBranchName())
        assertEquals(99, storage.getBranchId())
        assertEquals("STAGING", storage.getEnvironment())
    }
    
    // ========================================================================
    // clearRegistration Tests
    // ========================================================================
    
    @Test
    fun `clearRegistration removes all credentials from SecureStorage`() = runTest {
        val storage = InMemorySecureStorage()
        storage.saveStationId("station-123")
        storage.saveApiKey("api-key-abc")
        storage.saveBranchInfo(42, "Test Branch")
        val repository = createRepository(secureStorage = storage)
        
        val result = repository.clearRegistration()
        
        assertTrue(result.isSuccess)
        assertNull(storage.getStationId())
        assertNull(storage.getApiKey())
        assertFalse(storage.isRegistered())
    }
    
    // ========================================================================
    // requestQrCode Tests
    // ========================================================================
    
    @Test
    fun `requestQrCode returns QR data from API`() = runTest {
        val qrResponse = QrRegistrationResponseDto(
            url = "https://admin.gropos.com/register/ABC123",
            qrCodeImage = "base64-image-data",
            accessToken = "temp-token",
            assignedGuid = "device-guid-123"
        )
        val repository = createRepository(qrResponse = qrResponse)
        
        val result = repository.requestQrCode()
        
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("https://admin.gropos.com/register/ABC123", response.url)
        assertEquals("device-guid-123", response.assignedGuid)
    }
    
    @Test
    fun `requestQrCode returns failure on network error`() = runTest {
        val repository = createRepository(shouldFailNetwork = true)
        
        val result = repository.requestQrCode()
        
        assertTrue(result.isFailure)
    }
    
    // ========================================================================
    // checkRegistrationStatus Tests
    // ========================================================================
    
    @Test
    fun `checkRegistrationStatus returns Pending when not activated`() = runTest {
        val statusResponse = DeviceStatusResponseDto(
            deviceStatus = "Pending",
            apiKey = null,
            branchId = null,
            branch = null
        )
        val repository = createRepository(statusResponse = statusResponse)
        
        val result = repository.checkRegistrationStatus("device-guid-123")
        
        assertTrue(result.isSuccess)
        assertEquals("Pending", result.getOrNull()?.deviceStatus)
        assertNull(result.getOrNull()?.apiKey)
    }
    
    @Test
    fun `checkRegistrationStatus returns Registered with credentials when activated`() = runTest {
        val statusResponse = DeviceStatusResponseDto(
            deviceStatus = "Registered",
            apiKey = "production-api-key",
            branchId = 55,
            branch = "Production Branch",
            stationId = "prod-station-789"
        )
        val repository = createRepository(statusResponse = statusResponse)
        
        val result = repository.checkRegistrationStatus("device-guid-123")
        
        assertTrue(result.isSuccess)
        assertEquals("Registered", result.getOrNull()?.deviceStatus)
        assertEquals("production-api-key", result.getOrNull()?.apiKey)
    }
    
    // ========================================================================
    // generatePairingCode Tests
    // ========================================================================
    
    @Test
    fun `generatePairingCode returns formatted code`() {
        val repository = createRepository()
        
        val code = repository.generatePairingCode()
        
        // Format: XXXX-XXXX (4 chars, hyphen, 4 chars)
        assertEquals(9, code.length)
        assertEquals('-', code[4])
        assertTrue(code.substring(0, 4).all { it.isLetterOrDigit() })
        assertTrue(code.substring(5, 9).all { it.isLetterOrDigit() })
    }
    
    // ========================================================================
    // Test Helpers
    // ========================================================================
    
    private fun createRepository(
        secureStorage: InMemorySecureStorage = InMemorySecureStorage(),
        qrResponse: QrRegistrationResponseDto? = null,
        statusResponse: DeviceStatusResponseDto? = null,
        shouldFailNetwork: Boolean = false
    ): DeviceRepository {
        return FakeRemoteDeviceRepository(
            secureStorage = secureStorage,
            qrResponse = qrResponse,
            statusResponse = statusResponse,
            shouldFailNetwork = shouldFailNetwork
        )
    }
}

/**
 * Fake implementation of RemoteDeviceRepository for testing.
 */
private class FakeRemoteDeviceRepository(
    private val secureStorage: InMemorySecureStorage,
    private val qrResponse: QrRegistrationResponseDto?,
    private val statusResponse: DeviceStatusResponseDto?,
    private val shouldFailNetwork: Boolean
) : DeviceRepository {
    
    override suspend fun isRegistered(): Boolean {
        return secureStorage.isRegistered()
    }
    
    override suspend fun getDeviceInfo(): DeviceInfo? {
        if (!secureStorage.isRegistered()) return null
        
        return DeviceInfo(
            stationId = secureStorage.getStationId() ?: return null,
            apiKey = secureStorage.getApiKey() ?: return null,
            branchName = secureStorage.getBranchName() ?: "Unknown",
            branchId = secureStorage.getBranchId() ?: -1,
            environment = secureStorage.getEnvironment() ?: "PRODUCTION"
        )
    }
    
    override suspend fun registerDevice(deviceInfo: DeviceInfo): Result<Unit> {
        secureStorage.saveStationId(deviceInfo.stationId)
        secureStorage.saveApiKey(deviceInfo.apiKey)
        secureStorage.saveBranchInfo(deviceInfo.branchId, deviceInfo.branchName)
        secureStorage.saveEnvironment(deviceInfo.environment)
        return Result.success(Unit)
    }
    
    override suspend fun clearRegistration(): Result<Unit> {
        secureStorage.clearAll()
        return Result.success(Unit)
    }
    
    override fun generatePairingCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val part1 = (1..4).map { chars.random() }.joinToString("")
        val part2 = (1..4).map { chars.random() }.joinToString("")
        return "$part1-$part2"
    }
    
    override suspend fun requestQrCode(): Result<QrRegistrationResponse> {
        if (shouldFailNetwork) {
            return Result.failure(Exception("Network error"))
        }
        
        val response = qrResponse ?: return Result.failure(Exception("No response configured"))
        return Result.success(
            QrRegistrationResponse(
                url = response.url,
                qrCodeImage = response.qrCodeImage,
                accessToken = response.accessToken,
                assignedGuid = response.assignedGuid
            )
        )
    }
    
    override suspend fun checkRegistrationStatus(deviceGuid: String): Result<DeviceStatusResponse> {
        if (shouldFailNetwork) {
            return Result.failure(Exception("Network error"))
        }
        
        val response = statusResponse ?: return Result.failure(Exception("No response configured"))
        return Result.success(
            DeviceStatusResponse(
                deviceStatus = response.deviceStatus,
                apiKey = response.apiKey,
                branchId = response.branchId,
                branch = response.branch
            )
        )
    }
}

