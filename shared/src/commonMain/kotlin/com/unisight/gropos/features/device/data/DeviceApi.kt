package com.unisight.gropos.features.device.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.device.data.dto.CurrentDeviceInfoDto
import io.ktor.client.request.get
import io.ktor.client.request.url

/**
 * API interface for device-related operations.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - GET /api/v1/devices/current - Get current device info with claimed employee
 * 
 * This interface provides device status information including:
 * - Whether the station is claimed (employeeId set)
 * - What till is assigned (locationAccountId set)
 * - Branch and location information
 */
interface DeviceApi {
    
    /**
     * Gets current device info including claimed employee and till assignment.
     * 
     * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
     * - Returns employeeId if station is claimed by an employee
     * - Returns locationAccountId if a till is assigned
     * - Used for Station Claiming logic on login screen
     * 
     * @return Result containing device info, or error if failed
     */
    suspend fun getCurrentDevice(): Result<CurrentDeviceInfoDto>
}

/**
 * Remote implementation of DeviceApi using REST API.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - GET /api/v1/devices/current
 * - Requires x-api-key header (device authentication)
 * - Requires Authorization: Bearer header (user authentication)
 */
class RemoteDeviceApi(
    private val apiClient: ApiClient
) : DeviceApi {
    
    companion object {
        /**
         * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
         * GET /api/v1/devices/current - Get current device info
         */
        private const val ENDPOINT_DEVICE_CURRENT = "/api/v1/devices/current"
    }
    
    /**
     * Gets current device info from the backend.
     * 
     * This endpoint returns information about:
     * - Device/station identity (id, name, location)
     * - Claimed employee (employeeId, employee name)
     * - Assigned till (locationAccountId, locationAccount name)
     * - Last heartbeat timestamp
     * 
     * If employeeId is not null, the station is "claimed" and the
     * login screen should pre-select that employee.
     */
    override suspend fun getCurrentDevice(): Result<CurrentDeviceInfoDto> {
        val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_DEVICE_CURRENT
        println("[DeviceApi] Fetching current device info from $fullUrl")
        
        // Use authenticatedRequest for POS API endpoints
        return apiClient.authenticatedRequest {
            method = io.ktor.http.HttpMethod.Get
            url(fullUrl)
        }
    }
}

/**
 * Fake implementation for testing and development.
 * 
 * Simulates device states for testing station claiming logic.
 */
class FakeDeviceApi(
    private val simulatedDeviceInfo: CurrentDeviceInfoDto? = null
) : DeviceApi {
    
    override suspend fun getCurrentDevice(): Result<CurrentDeviceInfoDto> {
        return if (simulatedDeviceInfo != null) {
            Result.success(simulatedDeviceInfo)
        } else {
            // Default: Station is FREE (no claimed employee)
            Result.success(
                CurrentDeviceInfoDto(
                    id = 1,
                    branchId = 1,
                    branch = "Test Branch",
                    name = "Register 1",
                    location = "Front",
                    employeeId = null,  // FREE station
                    employee = null,
                    locationAccountId = null,
                    locationAccount = null,
                    lastHeartbeat = null
                )
            )
        }
    }
    
    /**
     * Creates a fake that simulates a CLAIMED station.
     */
    companion object {
        fun claimed(employeeId: Int, tillId: Int? = null): FakeDeviceApi {
            return FakeDeviceApi(
                CurrentDeviceInfoDto(
                    id = 1,
                    branchId = 1,
                    branch = "Test Branch",
                    name = "Register 1",
                    location = "Front",
                    employeeId = employeeId,
                    employee = "Claimed Employee",
                    locationAccountId = tillId,
                    locationAccount = tillId?.let { "Till $it" },
                    lastHeartbeat = null
                )
            )
        }
        
        fun free(): FakeDeviceApi = FakeDeviceApi(null)
    }
}

