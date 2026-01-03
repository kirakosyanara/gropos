package com.unisight.gropos.core.device

import kotlinx.coroutines.flow.StateFlow

/**
 * Device/Station information model.
 * 
 * Per DEVICE_REGISTRATION.md: Device info includes station ID, name, branch, and hardware config.
 * Per REMEDIATION_CHECKLIST: Station Name Display - show station identifier in headers.
 */
data class DeviceInfo(
    /** Unique device identifier (from registration) */
    val deviceId: String,
    
    /** Station/Register number (1, 2, 3, etc.) */
    val stationId: Int,
    
    /** Display name for the station (e.g., "Register 1", "Lane 3") */
    val stationName: String,
    
    /** Branch/Store ID */
    val branchId: Int,
    
    /** Branch/Store name */
    val branchName: String,
    
    /** Pre-assigned employee ID (if device is assigned to specific employee) */
    val preAssignedEmployeeId: Int? = null,
    
    /** Hardware configuration */
    val hardwareConfig: HardwareConfig = HardwareConfig()
)

/**
 * Hardware configuration for the device.
 * 
 * Per DESKTOP_HARDWARE.md: COM port settings for peripherals.
 */
data class HardwareConfig(
    /** COM port for barcode scanner (e.g., "COM3", "/dev/ttyUSB0") */
    val scannerPort: String? = null,
    
    /** COM port for receipt printer */
    val printerPort: String? = null,
    
    /** COM port for scale */
    val scalePort: String? = null,
    
    /** COM port for cash drawer */
    val cashDrawerPort: String? = null,
    
    /** Payment terminal connection info */
    val paymentTerminalIp: String? = null,
    
    /** Payment terminal port */
    val paymentTerminalPort: Int? = null,
    
    /** Whether to use simulated hardware (for development) */
    val useSimulatedHardware: Boolean = true
)

/**
 * Service for managing device/station information.
 * 
 * Per DEVICE_REGISTRATION.md: Provides device identity and configuration.
 */
interface DeviceService {
    
    /**
     * Current device information.
     */
    val deviceInfo: StateFlow<DeviceInfo>
    
    /**
     * Updates the device registration.
     */
    suspend fun register(deviceId: String, branchId: Int, stationId: Int): Result<Unit>
    
    /**
     * Updates the hardware configuration.
     */
    suspend fun updateHardwareConfig(config: HardwareConfig): Result<Unit>
    
    /**
     * Gets the station display name.
     */
    fun getStationDisplayName(): String
    
    /**
     * Gets the full location display (e.g., "Store #123 - Register 1").
     */
    fun getFullLocationDisplay(): String
}

/**
 * In-memory implementation of DeviceService for development/testing.
 */
class InMemoryDeviceService(
    initialDeviceInfo: DeviceInfo = DeviceInfo(
        deviceId = "DEV-001",
        stationId = 1,
        stationName = "Register 1",
        branchId = 1,
        branchName = "Main Store"
    )
) : DeviceService {
    
    private val _deviceInfo = kotlinx.coroutines.flow.MutableStateFlow(initialDeviceInfo)
    override val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo
    
    override suspend fun register(deviceId: String, branchId: Int, stationId: Int): Result<Unit> {
        _deviceInfo.value = _deviceInfo.value.copy(
            deviceId = deviceId,
            branchId = branchId,
            stationId = stationId,
            stationName = "Register $stationId"
        )
        return Result.success(Unit)
    }
    
    override suspend fun updateHardwareConfig(config: HardwareConfig): Result<Unit> {
        _deviceInfo.value = _deviceInfo.value.copy(hardwareConfig = config)
        return Result.success(Unit)
    }
    
    override fun getStationDisplayName(): String {
        return _deviceInfo.value.stationName
    }
    
    override fun getFullLocationDisplay(): String {
        val info = _deviceInfo.value
        return "${info.branchName} - ${info.stationName}"
    }
    
    // Test helpers
    fun setStationName(name: String) {
        _deviceInfo.value = _deviceInfo.value.copy(stationName = name)
    }
    
    fun setBranchName(name: String) {
        _deviceInfo.value = _deviceInfo.value.copy(branchName = name)
    }
}

