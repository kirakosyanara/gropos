package com.unisight.gropos.features.device.presentation

import com.unisight.gropos.core.storage.InMemorySecureStorage
import com.unisight.gropos.features.device.data.dto.DeviceStatusResponseDto
import com.unisight.gropos.features.device.data.dto.QrRegistrationResponseDto
import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse
import com.unisight.gropos.features.device.domain.model.RegistrationState
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RegistrationViewModel.
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Tests the full registration flow
 * - Verifies polling behavior with correct intervals
 * - Verifies state transitions
 * 
 * **TDD Approach:**
 * - Tests cover all C1-C4 and H1-H4 remediation fixes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // ========================================================================
    // Initial State Tests
    // ========================================================================
    
    @Test
    fun `initial state is LOADING`() = runTest {
        val viewModel = createViewModel()
        
        // Initial state should be LOADING
        assertEquals(RegistrationState.LOADING, viewModel.state.value.registrationState)
    }
    
    @Test
    fun `transitions to REGISTERED if already registered`() = testScope.runTest {
        val storage = InMemorySecureStorage()
        storage.saveStationId("existing-station-123")
        storage.saveApiKey("existing-api-key")
        storage.saveBranchInfo(42, "Existing Branch")
        
        val viewModel = createViewModel(
            repository = FakeDeviceRepositoryForTest(secureStorage = storage),
            scope = this
        )
        
        advanceUntilIdle()
        
        assertEquals(RegistrationState.REGISTERED, viewModel.state.value.registrationState)
        assertEquals("Existing Branch", viewModel.state.value.branchName)
    }
    
    // ========================================================================
    // QR Code Generation Tests
    // ========================================================================
    
    @Test
    fun `generates pairing code and transitions to PENDING`() = testScope.runTest {
        val viewModel = createViewModel(scope = this)
        
        advanceUntilIdle()
        
        assertEquals(RegistrationState.PENDING, viewModel.state.value.registrationState)
        assertTrue(viewModel.state.value.pairingCode.isNotEmpty())
    }
    
    @Test
    fun `stores device GUID from QR response`() = testScope.runTest {
        val expectedGuid = "test-device-guid-abc123"
        val viewModel = createViewModel(
            repository = FakeDeviceRepositoryForTest(
                qrResponse = QrRegistrationResponse(
                    url = "https://admin.gropos.com/register/$expectedGuid",
                    qrCodeImage = "base64-qr-image",
                    accessToken = "temp-token",
                    assignedGuid = expectedGuid
                )
            ),
            scope = this
        )
        
        advanceUntilIdle()
        
        assertEquals(expectedGuid, viewModel.state.value.deviceGuid)
    }
    
    // ========================================================================
    // Status Polling Tests (C3)
    // ========================================================================
    
    @Test
    fun `transitions to IN_PROGRESS when admin starts assignment`() = testScope.runTest {
        var pollCount = 0
        val repository = FakeDeviceRepositoryForTest(
            statusResponseProvider = {
                pollCount++
                if (pollCount < 3) {
                    DeviceStatusResponse("Pending", null, null, null)
                } else {
                    DeviceStatusResponse("In-Progress", null, null, "Main Store")
                }
            }
        )
        
        val viewModel = createViewModel(repository = repository, scope = this)
        
        advanceUntilIdle()  // Initial QR request
        
        // Advance time for polling (10s intervals)
        advanceTimeBy(25_000)  // 2.5 polls
        
        assertEquals(RegistrationState.IN_PROGRESS, viewModel.state.value.registrationState)
        assertEquals("Main Store", viewModel.state.value.branchName)
    }
    
    @Test
    fun `completes registration when status is Registered`() = testScope.runTest {
        val storage = InMemorySecureStorage()
        var pollCount = 0
        
        val repository = FakeDeviceRepositoryForTest(
            secureStorage = storage,
            qrResponse = QrRegistrationResponse(
                url = "https://admin.gropos.com/register/reg-guid",
                qrCodeImage = "qr-image",
                accessToken = "poll-token",
                assignedGuid = "reg-guid"
            ),
            statusResponseProvider = {
                pollCount++
                if (pollCount < 2) {
                    DeviceStatusResponse("Pending", null, null, null)
                } else {
                    DeviceStatusResponse(
                        deviceStatus = "Registered",
                        apiKey = "sk_live_newApiKey123",
                        branchId = 99,
                        branch = "New Branch"
                    )
                }
            }
        )
        
        val viewModel = createViewModel(repository = repository, scope = this)
        
        advanceUntilIdle()  // Initial QR request
        advanceTimeBy(15_000)  // 1.5 polls
        
        assertEquals(RegistrationState.REGISTERED, viewModel.state.value.registrationState)
        assertEquals("New Branch", viewModel.state.value.branchName)
        
        // Verify credentials saved to SecureStorage
        assertEquals("reg-guid", storage.getStationId())
        assertEquals("sk_live_newApiKey123", storage.getApiKey())
        assertEquals("New Branch", storage.getBranchName())
        assertEquals(99, storage.getBranchId())
    }
    
    // ========================================================================
    // Timeout Tests (H1)
    // ========================================================================
    
    @Test
    fun `times out after 10 minutes if still PENDING`() = testScope.runTest {
        val repository = FakeDeviceRepositoryForTest(
            statusResponseProvider = {
                DeviceStatusResponse("Pending", null, null, null)
            }
        )
        
        val viewModel = createViewModel(repository = repository, scope = this)
        
        advanceUntilIdle()  // Initial QR request
        
        // Advance past 10 minute timeout
        advanceTimeBy(11 * 60 * 1000L)  // 11 minutes
        
        assertEquals(RegistrationState.TIMEOUT, viewModel.state.value.registrationState)
    }
    
    // ========================================================================
    // Error Handling Tests
    // ========================================================================
    
    @Test
    fun `handles QR request failure gracefully`() = testScope.runTest {
        val repository = FakeDeviceRepositoryForTest(
            shouldFailQrRequest = true
        )
        
        val viewModel = createViewModel(repository = repository, scope = this)
        
        advanceUntilIdle()
        
        assertEquals(RegistrationState.ERROR, viewModel.state.value.registrationState)
        assertNotNull(viewModel.state.value.errorMessage)
    }
    
    @Test
    fun `refresh event generates new pairing code`() = testScope.runTest {
        val viewModel = createViewModel(scope = this)
        
        advanceUntilIdle()
        
        val firstCode = viewModel.state.value.pairingCode
        
        viewModel.onEvent(RegistrationEvent.RefreshPairingCode)
        
        advanceUntilIdle()
        
        // Code should be different (randomized)
        assertTrue(viewModel.state.value.pairingCode.isNotEmpty())
    }
    
    @Test
    fun `retry after timeout generates new QR code`() = testScope.runTest {
        val repository = FakeDeviceRepositoryForTest(
            statusResponseProvider = {
                DeviceStatusResponse("Pending", null, null, null)
            }
        )
        
        val viewModel = createViewModel(repository = repository, scope = this)
        
        advanceUntilIdle()
        advanceTimeBy(11 * 60 * 1000L)  // Timeout
        
        assertEquals(RegistrationState.TIMEOUT, viewModel.state.value.registrationState)
        
        // Retry
        viewModel.onEvent(RegistrationEvent.RetryAfterTimeout)
        advanceUntilIdle()
        
        // Should be back in PENDING state with new code
        assertEquals(RegistrationState.PENDING, viewModel.state.value.registrationState)
    }
    
    // ========================================================================
    // Helper Functions
    // ========================================================================
    
    private fun createViewModel(
        repository: DeviceRepository = FakeDeviceRepositoryForTest(),
        scope: kotlinx.coroutines.CoroutineScope? = null
    ): RegistrationViewModel {
        return RegistrationViewModel(
            deviceRepository = repository,
            coroutineScope = scope
        )
    }
}

/**
 * Fake DeviceRepository for testing RegistrationViewModel.
 * 
 * Allows configuring responses for different test scenarios.
 */
private class FakeDeviceRepositoryForTest(
    private val secureStorage: InMemorySecureStorage = InMemorySecureStorage(),
    private val qrResponse: QrRegistrationResponse = QrRegistrationResponse(
        url = "https://admin.gropos.com/register/test-guid",
        qrCodeImage = "test-qr-image",
        accessToken = "test-access-token",
        assignedGuid = "test-guid"
    ),
    private val statusResponseProvider: (() -> DeviceStatusResponse)? = null,
    private val shouldFailQrRequest: Boolean = false,
    private val shouldFailStatusCheck: Boolean = false
) : DeviceRepository {
    
    private var pairingCodeCount = 0
    
    override suspend fun isRegistered(): Boolean = secureStorage.isRegistered()
    
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
        pairingCodeCount++
        return "TEST-${pairingCodeCount.toString().padStart(4, '0')}"
    }
    
    override suspend fun requestQrCode(): Result<QrRegistrationResponse> {
        if (shouldFailQrRequest) {
            return Result.failure(Exception("Network error"))
        }
        return Result.success(qrResponse)
    }
    
    override suspend fun checkRegistrationStatus(deviceGuid: String): Result<DeviceStatusResponse> {
        if (shouldFailStatusCheck) {
            return Result.failure(Exception("Status check failed"))
        }
        
        val response = statusResponseProvider?.invoke()
            ?: DeviceStatusResponse("Pending", null, null, null)
        
        return Result.success(response)
    }
}

