package com.unisight.gropos.features.device.data

import com.unisight.gropos.features.device.data.dto.DeviceStatusResponseDto
import com.unisight.gropos.features.device.data.dto.HeartbeatResponse
import com.unisight.gropos.features.device.data.dto.QrRegistrationRequest
import com.unisight.gropos.features.device.data.dto.QrRegistrationResponseDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for Device Registration DTOs.
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Validates correct JSON serialization per API spec
 * - Ensures payload alignment with backend requirements
 * 
 * **TDD Approach:**
 * - Tests written BEFORE implementation fixes
 * - Each test validates a specific remediation item
 */
class DeviceRegistrationDtoTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ========================================================================
    // C1: QrRegistrationRequest - Must have deviceType: Int (not deviceName, platform)
    // ========================================================================
    
    @Test
    fun `C1 - QrRegistrationRequest serializes with deviceType field`() {
        // Per DEVICE_REGISTRATION.md Section 4.1:
        // Request Body: { "deviceType": 0 }
        
        val request = QrRegistrationRequest(deviceType = 0)
        
        val jsonString = json.encodeToString(request)
        
        // Must contain "deviceType": 0
        assertEquals("""{"deviceType":0}""", jsonString)
    }
    
    @Test
    fun `C1 - QrRegistrationRequest defaults deviceType to 0 for GroPOS`() {
        // Per DEVICE_REGISTRATION.md: deviceType 0 = GroPOS (default)
        
        val request = QrRegistrationRequest()
        
        assertEquals(0, request.deviceType)
    }
    
    @Test
    fun `C1 - QrRegistrationRequest does NOT contain deviceName or platform fields`() {
        // Per DEVICE_REGISTRATION.md: These fields are NOT in the API spec
        
        val request = QrRegistrationRequest(deviceType = 0)
        val jsonString = json.encodeToString(request)
        
        // Must NOT contain legacy fields
        assert(!jsonString.contains("deviceName")) { 
            "Request should not contain 'deviceName' field" 
        }
        assert(!jsonString.contains("platform")) { 
            "Request should not contain 'platform' field" 
        }
    }
    
    // ========================================================================
    // QrRegistrationResponse - Verify all expected fields
    // ========================================================================
    
    @Test
    fun `QrRegistrationResponse parses all fields correctly`() {
        // Per DEVICE_REGISTRATION.md Section 4.1 Response
        val jsonResponse = """
            {
                "url": "https://admin.gropos.com/register/ABC123XYZ",
                "qrCodeImage": "iVBORw0KGgoAAAANSUhEUgAA...",
                "accessToken": "eyJhbGciOiJIUzI1NiIs...",
                "assignedGuid": "550e8400-e29b-41d4-a716-446655440000"
            }
        """.trimIndent()
        
        val response = json.decodeFromString<QrRegistrationResponseDto>(jsonResponse)
        
        assertEquals("https://admin.gropos.com/register/ABC123XYZ", response.url)
        assertEquals("iVBORw0KGgoAAAANSUhEUgAA...", response.qrCodeImage)
        assertEquals("eyJhbGciOiJIUzI1NiIs...", response.accessToken)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.assignedGuid)
    }
    
    // ========================================================================
    // C4: DeviceStatusResponse - stationId should NOT be in API response
    // ========================================================================
    
    @Test
    fun `C4 - DeviceStatusResponse parses Registered status with credentials`() {
        // Per DEVICE_REGISTRATION.md Section 4.2
        val jsonResponse = """
            {
                "deviceStatus": "Registered",
                "apiKey": "<DEVICE_API_KEY>",
                "branchId": 42,
                "branch": "Main Street Store"
            }
        """.trimIndent()
        
        val response = json.decodeFromString<DeviceStatusResponseDto>(jsonResponse)
        
        assertEquals("Registered", response.deviceStatus)
        assertEquals("<DEVICE_API_KEY>", response.apiKey)
        assertEquals(42, response.branchId)
        assertEquals("Main Street Store", response.branch)
    }
    
    @Test
    fun `C4 - DeviceStatusResponse parses Pending status with null credentials`() {
        // Per DEVICE_REGISTRATION.md Section 4.2
        val jsonResponse = """
            {
                "deviceStatus": "Pending",
                "apiKey": null,
                "branchId": null,
                "branch": null
            }
        """.trimIndent()
        
        val response = json.decodeFromString<DeviceStatusResponseDto>(jsonResponse)
        
        assertEquals("Pending", response.deviceStatus)
        assertNull(response.apiKey)
        assertNull(response.branchId)
        assertNull(response.branch)
    }
    
    @Test
    fun `C4 - DeviceStatusResponse parses In-Progress status with branch name`() {
        // Per DEVICE_REGISTRATION.md Section 4.2
        val jsonResponse = """
            {
                "deviceStatus": "In-Progress",
                "apiKey": null,
                "branchId": null,
                "branch": "Main Street Store"
            }
        """.trimIndent()
        
        val response = json.decodeFromString<DeviceStatusResponseDto>(jsonResponse)
        
        assertEquals("In-Progress", response.deviceStatus)
        assertNull(response.apiKey)
        assertEquals("Main Street Store", response.branch)
    }
    
    // ========================================================================
    // H2: HeartbeatResponse - Must match GET response (messageCount only)
    // ========================================================================
    
    @Test
    fun `H2 - HeartbeatResponse parses messageCount correctly`() {
        // Per DEVICE_REGISTRATION.md Section 4.3
        // GET /device-registration/heartbeat response: { "messageCount": 5 }
        val jsonResponse = """{"messageCount": 5}"""
        
        val response = json.decodeFromString<HeartbeatResponse>(jsonResponse)
        
        assertEquals(5, response.messageCount)
    }
    
    @Test
    fun `H2 - HeartbeatResponse defaults messageCount to 0`() {
        val response = HeartbeatResponse()
        
        assertEquals(0, response.messageCount)
    }
}

