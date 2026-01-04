package com.unisight.gropos.features.device.data.dto

import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Device type constants per DEVICE_REGISTRATION.md Section 4.1.
 * 
 * Used in QrRegistrationRequest to identify the device type.
 */
object DeviceTypes {
    const val GROPOS = 0
    const val ONE_TIME = 1
    const val ONE_STORE = 2
    const val ONE_SERVER = 3
    const val SCALE = 4
    const val ESL_SERVER = 5
    const val ESL_TAG = 6
    const val DASHBOARD = 7
    const val REGISTER_SCALE_CAMERA = 8
    const val PRINTING_SCALE_CAMERA = 9
    const val SHRINK_PRODUCTION_CAMERA = 10
    const val ONE_SCANNER = 11
    const val ONE_PAY = 12
    const val ONE_POINT = 13
}

/**
 * Request body for QR code registration.
 * 
 * **Per DEVICE_REGISTRATION.md Section 4.1:**
 * - API Endpoint: POST /device-registration/qr-registration
 * - Request Body: { "deviceType": 0 }
 * - deviceType 0 = GroPOS (default)
 * 
 * **C1 FIX:** Changed from (deviceName, platform) to (deviceType) per API spec.
 */
@Serializable
data class QrRegistrationRequest(
    @SerialName("deviceType")
    val deviceType: Int = DeviceTypes.GROPOS
)

/**
 * Response from QR code registration API.
 * 
 * **API Endpoint:** POST /device-registration/qr-registration
 */
@Serializable
data class QrRegistrationResponseDto(
    @SerialName("url")
    val url: String? = null,
    
    @SerialName("qrCodeImage")
    val qrCodeImage: String? = null,
    
    @SerialName("accessToken")
    val accessToken: String? = null,
    
    @SerialName("assignedGuid")
    val assignedGuid: String? = null
)

/**
 * Response from device status polling API.
 * 
 * **Per DEVICE_REGISTRATION.md Section 4.2:**
 * - API Endpoint: GET /device-registration/device-status/{deviceGuid}
 * - Headers: Authorization: Bearer <accessToken>, version: v1
 * 
 * **C4 FIX:** Removed stationId field - it's not in the API response.
 * The stationId should come from assignedGuid in QrRegistrationResponse.
 */
@Serializable
data class DeviceStatusResponseDto(
    @SerialName("deviceStatus")
    val deviceStatus: String? = null,
    
    @SerialName("apiKey")
    val apiKey: String? = null,
    
    @SerialName("branchId")
    val branchId: Int? = null,
    
    @SerialName("branch")
    val branch: String? = null
    // C4 FIX: stationId removed - use assignedGuid from QR response as stationId
)

/**
 * Response from device heartbeat.
 * 
 * **Per DEVICE_REGISTRATION.md Section 4.3:**
 * - API Endpoint: GET /device-registration/heartbeat (not POST!)
 * - Headers: x-api-key: <apiKey>, version: v1
 * - Response: { "messageCount": 5 }
 * 
 * **H2 FIX:** Changed from POST with body to GET with simple response.
 * Removed DeviceHeartbeatRequest - heartbeat is a GET with no body.
 */
@Serializable
data class HeartbeatResponse(
    @SerialName("messageCount")
    val messageCount: Int = 0
)

/**
 * Current device info response from /api/v1/devices/current.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - GET /api/v1/devices/current
 * - Returns device info INCLUDING claimed employee and till assignment
 * - Used for Station Claiming logic on login screen
 * 
 * When employeeId is not null, the station is "claimed" by that employee
 * and the login screen should pre-select them.
 */
@Serializable
data class CurrentDeviceInfoDto(
    @SerialName("id")
    val id: Int,
    
    @SerialName("branchId")
    val branchId: Int,
    
    @SerialName("branch")
    val branch: String? = null,
    
    @SerialName("name")
    val name: String? = null,
    
    @SerialName("location")
    val location: String? = null,
    
    @SerialName("employeeId")
    val employeeId: Int? = null,  // ID of claimed employee (null if station is FREE)
    
    @SerialName("employee")
    val employee: String? = null,  // Name of claimed employee
    
    @SerialName("locationAccountId")
    val locationAccountId: Int? = null,  // Assigned till account ID
    
    @SerialName("locationAccount")
    val locationAccount: String? = null,  // Till account name
    
    @SerialName("lastHeartbeat")
    val lastHeartbeat: String? = null
)

/**
 * Mapper for converting between Device DTOs and Domain models.
 */
object DeviceDomainMapper {
    
    fun QrRegistrationResponseDto.toDomain(): QrRegistrationResponse {
        return QrRegistrationResponse(
            url = url,
            qrCodeImage = qrCodeImage,
            accessToken = accessToken,
            assignedGuid = assignedGuid
        )
    }
    
    fun DeviceStatusResponseDto.toDomain(): DeviceStatusResponse {
        return DeviceStatusResponse(
            deviceStatus = deviceStatus,
            apiKey = apiKey,
            branchId = branchId,
            branch = branch
        )
    }
    
    /**
     * Creates DeviceInfo from a successful registration status response.
     * 
     * **C4 FIX:** stationId must be provided from assignedGuid (from QR response).
     * The status response does NOT contain stationId per API spec.
     * 
     * @param assignedGuid The device GUID from the initial QR registration response
     */
    fun DeviceStatusResponseDto.toDeviceInfo(assignedGuid: String): DeviceInfo? {
        if (deviceStatus != "Registered" || apiKey.isNullOrEmpty() || assignedGuid.isEmpty()) {
            return null
        }
        
        return DeviceInfo(
            stationId = assignedGuid,  // C4 FIX: Use assignedGuid as stationId
            apiKey = apiKey,
            branchName = branch ?: "Unknown Branch",
            branchId = branchId ?: -1,
            environment = "PRODUCTION",
            registeredAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }
}

