package com.unisight.gropos.features.device.data.dto

import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
import com.unisight.gropos.features.device.domain.model.QrRegistrationResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for device registration.
 * 
 * **API Endpoint:** POST /device/register
 */
@Serializable
data class DeviceRegisterRequest(
    @SerialName("pairingCode")
    val pairingCode: String,
    
    @SerialName("deviceName")
    val deviceName: String,
    
    @SerialName("platform")
    val platform: String // "ANDROID", "DESKTOP", "WEB"
)

/**
 * Request body for QR code registration.
 * 
 * **API Endpoint:** POST /device-registration/qr-registration
 */
@Serializable
data class QrRegistrationRequest(
    @SerialName("deviceName")
    val deviceName: String,
    
    @SerialName("platform")
    val platform: String
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
 * **API Endpoint:** GET /device-registration/device-status/{deviceGuid}
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
    val branch: String? = null,
    
    @SerialName("stationId")
    val stationId: String? = null
)

/**
 * Request body for device heartbeat.
 * 
 * **API Endpoint:** POST /device/heartbeat
 */
@Serializable
data class DeviceHeartbeatRequest(
    @SerialName("stationId")
    val stationId: String,
    
    @SerialName("appVersion")
    val appVersion: String,
    
    @SerialName("batteryLevel")
    val batteryLevel: Int? = null,
    
    @SerialName("isOnline")
    val isOnline: Boolean = true
)

/**
 * Response from device heartbeat.
 */
@Serializable
data class DeviceHeartbeatResponse(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("message")
    val message: String? = null,
    
    @SerialName("serverTime")
    val serverTime: String? = null
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
     */
    fun DeviceStatusResponseDto.toDeviceInfo(): DeviceInfo? {
        if (deviceStatus != "Registered" || apiKey.isNullOrEmpty() || stationId.isNullOrEmpty()) {
            return null
        }
        
        return DeviceInfo(
            stationId = stationId,
            apiKey = apiKey,
            branchName = branch ?: "Unknown Branch",
            branchId = branchId ?: -1,
            environment = "PRODUCTION",
            registeredAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }
}

