package com.unisight.gropos.features.device.presentation

import com.unisight.gropos.core.AppConstants
import com.unisight.gropos.features.device.domain.model.RegistrationState

/**
 * UI State for the Registration Screen.
 * 
 * Per DEVICE_REGISTRATION.md:
 * - Shows QR code and pairing code for device activation
 * - Polls for registration status
 * - Transitions to login on successful activation
 */
data class RegistrationUiState(
    /** Current registration state */
    val registrationState: RegistrationState = RegistrationState.UNREGISTERED,
    
    /** Station name (placeholder until registered) */
    val stationName: String = "Unregistered Station",
    
    /** Current time display */
    val currentTime: String = "",
    
    /** App version string */
    val version: String = AppConstants.APP_VERSION,
    
    /** Generated pairing code (format: XXXX-XXXX) */
    val pairingCode: String = "",
    
    /** URL for admin portal activation */
    val activationUrl: String = "admin.gropos.com",
    
    /** Base64 QR code image (null = show placeholder) */
    val qrCodeImage: String? = null,
    
    /** Device GUID assigned during QR request */
    val deviceGuid: String? = null,
    
    /** Branch name (shown during IN_PROGRESS state) */
    val branchName: String = "",
    
    /** Loading state */
    val isLoading: Boolean = false,
    
    /** Error message */
    val errorMessage: String? = null,
    
    /** Show admin settings dialog */
    val showAdminSettings: Boolean = false,
    
    /** Is dev mode enabled (shows "Simulate Activation" button) */
    val isDevMode: Boolean = true // Default to true for P2 development
)

/**
 * Events for Registration Screen actions
 */
sealed interface RegistrationEvent {
    /** Generate new QR code / pairing code */
    data object RefreshPairingCode : RegistrationEvent
    
    /** Dev tool: Simulate successful activation */
    data object SimulateActivation : RegistrationEvent
    
    /** Show admin settings dialog */
    data object ShowAdminSettings : RegistrationEvent
    
    /** Hide admin settings dialog */
    data object HideAdminSettings : RegistrationEvent
    
    /** Dismiss error message */
    data object DismissError : RegistrationEvent
}

