package com.unisight.gropos.features.auth.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.storage.SecureStorage
import com.unisight.gropos.core.sync.InitialSyncService
import com.unisight.gropos.core.sync.SyncProgress
import com.unisight.gropos.core.sync.SyncState
import com.unisight.gropos.features.auth.domain.hardware.NfcResult
import com.unisight.gropos.features.auth.domain.hardware.NfcScanner
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import com.unisight.gropos.features.device.data.DeviceApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ScreenModel (ViewModel) for the Login screen.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * Implements the login state machine:
 * LOADING -> SYNCING -> EMPLOYEE_SELECT -> PIN_ENTRY -> TILL_ASSIGNMENT -> SUCCESS
 * 
 * **Per COUCHBASE_SYNCHRONIZATION_DETAILED.md:**
 * - On initial load (or after database wipe), shows SYNCING stage
 * - Displays detailed progress to the user
 * - Only syncs once per installation (tracked via SecureStorage)
 * 
 * **Station Claiming Logic (L1):**
 * - Calls GET /api/v1/devices/current to check if station is claimed
 * - If deviceInfo.employeeId is set, pre-selects that employee
 * - If deviceInfo.locationAccountId is set, pre-assigns that till
 * 
 * Per ANDROID_HARDWARE_GUIDE.md:
 * Supports NFC badge login as alternative to PIN entry.
 * 
 * @param employeeRepository Repository for fetching employees
 * @param tillRepository Repository for till management
 * @param nfcScanner Hardware abstraction for NFC badge readers
 * @param deviceApi API for device status (station claiming)
 * @param initialSyncService Service for initial data synchronization
 * @param secureStorage Storage for tracking sync completion status
 * @param coroutineScope Scope for launching coroutines (injectable for tests)
 */
class LoginViewModel(
    private val employeeRepository: EmployeeRepository,
    private val tillRepository: TillRepository,
    private val nfcScanner: NfcScanner,
    private val deviceApi: DeviceApi,
    private val initialSyncService: InitialSyncService? = null,
    private val secureStorage: SecureStorage? = null,
    private val coroutineScope: CoroutineScope? = null
) : ScreenModel {
    
    private val scope: CoroutineScope
        get() = coroutineScope ?: screenModelScope
    
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()
    
    init {
        println("[LoginViewModel] INIT - Starting...")
        println("[LoginViewModel] INIT - initialSyncService: ${if (initialSyncService != null) "AVAILABLE" else "NULL"}")
        println("[LoginViewModel] INIT - secureStorage: ${if (secureStorage != null) "AVAILABLE" else "NULL"}")
        checkAndPerformInitialSync()
    }
    
    /**
     * Check if initial sync is needed and perform it if necessary.
     * 
     * **Per COUCHBASE_SYNCHRONIZATION_DETAILED.md:**
     * - Sync is needed on first launch (isInitialSyncCompleted = false)
     * - Sync is needed after database wipe (flag is cleared)
     * - Skip sync on subsequent logins
     */
    private fun checkAndPerformInitialSync() {
        scope.launch {
            val syncCompleted = secureStorage?.isInitialSyncCompleted() ?: false
            
            println("[LoginViewModel] Checking sync status...")
            println("[LoginViewModel]   secureStorage available: ${secureStorage != null}")
            println("[LoginViewModel]   initialSyncService available: ${initialSyncService != null}")
            println("[LoginViewModel]   Initial sync completed: $syncCompleted")
            
            if (!syncCompleted && initialSyncService != null) {
                // Need to perform initial sync
                println("[LoginViewModel] >>> TRIGGERING SYNC <<<")
                performInitialSync()
            } else {
                // Sync already done, load employees directly
                println("[LoginViewModel] Skipping sync - loading employees directly")
                loadEmployees()
            }
        }
    }
    
    /**
     * Perform initial data synchronization.
     * 
     * Shows SYNCING stage with progress updates.
     * Ensures user sees the sync screen for at least 2 seconds.
     */
    private suspend fun performInitialSync() {
        println("[LoginViewModel] Starting initial sync...")
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        // Transition to SYNCING stage
        _state.update { 
            it.copy(
                stage = LoginStage.SYNCING,
                syncProgress = SyncProgressUiModel(
                    isActive = true,
                    currentStep = 0,
                    totalSteps = 13,
                    statusMessage = "Connecting to server..."
                )
            )
        }
        
        println("[LoginViewModel] Set stage to SYNCING")
        
        // Small delay to ensure UI renders the SYNCING stage
        kotlinx.coroutines.delay(100)
        
        // Collect sync progress updates
        val syncService = initialSyncService ?: run {
            // No sync service available, skip to employee loading
            println("[LoginViewModel] No sync service, skipping to employee loading")
            loadEmployees()
            return
        }
        
        // Subscribe to progress updates in background
        val progressJob = scope.launch {
            syncService.progress.collect { progress ->
                println("[LoginViewModel] Sync progress: step ${progress.currentStep}/${progress.totalSteps} - ${progress.currentEntity}")
                updateSyncProgress(progress)
            }
        }
        
        // Perform the sync
        println("[LoginViewModel] Calling syncService.performInitialSync()...")
        val result = syncService.performInitialSync()
        
        // Calculate how long the sync took
        val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
        println("[LoginViewModel] Sync took ${elapsed}ms")
        
        // Ensure minimum display time of 2 seconds so user can see the UI
        val minDisplayTime = 2000L
        if (elapsed < minDisplayTime) {
            val remaining = minDisplayTime - elapsed
            println("[LoginViewModel] Waiting ${remaining}ms for minimum display time")
            kotlinx.coroutines.delay(remaining)
        }
        
        progressJob.cancel()
        
        result.fold(
            onSuccess = {
                println("[LoginViewModel] Initial sync completed successfully")
                
                // Mark sync as completed
                secureStorage?.saveInitialSyncCompleted(true)
                secureStorage?.saveLastSyncTimestamp(Clock.System.now().toEpochMilliseconds())
                
                // Update UI with completion
                _state.update {
                    it.copy(
                        syncProgress = it.syncProgress.copy(
                            currentStep = it.syncProgress.totalSteps,
                            statusMessage = "Sync complete!",
                            isActive = false
                        )
                    )
                }
                
                // Small delay to show completion, then load employees
                kotlinx.coroutines.delay(500)
                loadEmployees()
            },
            onFailure = { error ->
                println("[LoginViewModel] Initial sync failed: ${error.message}")
                
                // Show error but allow continuing
                _state.update {
                    it.copy(
                        syncProgress = it.syncProgress.copy(
                            hasError = true,
                            errorMessage = error.message ?: "Sync failed. Please try again.",
                            statusMessage = "Sync Failed",
                            isActive = false
                        )
                    )
                }
                
                // Still try to load employees (offline mode)
                kotlinx.coroutines.delay(2000)
                loadEmployees()
            }
        )
    }
    
    /**
     * Update UI with sync progress.
     */
    private fun updateSyncProgress(progress: SyncProgress) {
        _state.update {
            it.copy(
                syncProgress = SyncProgressUiModel(
                    isActive = true,
                    currentStep = progress.currentStep,
                    totalSteps = progress.totalSteps,
                    currentEntity = progress.currentEntity,
                    statusMessage = progress.statusMessage,
                    hasError = false
                )
            )
        }
    }
    
    /**
     * Load scheduled employees for this station.
     * Called on ViewModel initialization.
     * 
     * **Per LOCK_SCREEN_AND_CASHIER_LOGIN.md (L1: Station Claiming):**
     * 1. First, call GET /api/v1/devices/current to check if station is claimed
     * 2. If deviceInfo.employeeId is set, pre-select that employee
     * 3. If deviceInfo.locationAccountId is set, pre-assign that till
     * 4. Otherwise, show full employee selection
     */
    private fun loadEmployees() {
        scope.launch {
            _state.update { it.copy(isLoading = true, stage = LoginStage.LOADING) }
            
            // STEP 1: Get device info to check for claimed employee (L1)
            val deviceInfoResult = deviceApi.getCurrentDevice()
            val deviceInfo = deviceInfoResult.getOrNull()
            
            println("[LoginViewModel] Device info: employeeId=${deviceInfo?.employeeId}, tillId=${deviceInfo?.locationAccountId}")
            
            // STEP 2: Load employees
            employeeRepository.getEmployees()
                .onSuccess { employees ->
                    val employeeUiModels = employees.map { emp -> emp.toUiModel() }
                    
                    // STEP 3: Check if station is claimed (L1)
                    if (deviceInfo?.employeeId != null) {
                        val claimedEmployee = employeeUiModels.find { 
                            it.id == deviceInfo.employeeId 
                        }
                        
                        if (claimedEmployee != null) {
                            println("[LoginViewModel] Station CLAIMED by employee: ${claimedEmployee.fullName}")
                            
                            // Pre-select claimed employee with till assignment if available
                            val employeeWithTill = claimedEmployee.copy(
                                assignedTillId = deviceInfo.locationAccountId ?: claimedEmployee.assignedTillId
                            )
                            
                            _state.update {
                                it.copy(
                                    employees = employeeUiModels,
                                    selectedEmployee = employeeWithTill,
                                    stage = LoginStage.PIN_ENTRY, // Skip to PIN entry
                                    isLoading = false,
                                    isStationClaimed = true,
                                    claimedTillId = deviceInfo.locationAccountId,
                                    currentTime = getCurrentTime()
                                )
                            }
                            return@onSuccess
                        }
                    }
                    
                    // Station is FREE - show employee selection
                    println("[LoginViewModel] Station FREE - showing employee selection")
                    _state.update { 
                        it.copy(
                            employees = employeeUiModels,
                            stage = LoginStage.EMPLOYEE_SELECT,
                            isLoading = false,
                            isStationClaimed = false,
                            claimedTillId = null,
                            currentTime = getCurrentTime()
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            errorMessage = error.message ?: "Failed to load employees",
                            isLoading = false,
                            stage = LoginStage.EMPLOYEE_SELECT
                        )
                    }
                }
        }
    }
    
    /**
     * User selects an employee from the grid.
     * Transitions to PIN_ENTRY stage.
     */
    fun onEmployeeSelected(employee: EmployeeUiModel) {
        _state.update { 
            it.copy(
                selectedEmployee = employee,
                stage = LoginStage.PIN_ENTRY,
                pinInput = "",
                errorMessage = null
            )
        }
    }
    
    /**
     * User presses a digit on the PIN pad.
     */
    fun onPinDigit(digit: String) {
        val currentPin = _state.value.pinInput
        if (currentPin.length < 20) { // Max 20 digits per spec
            _state.update { it.copy(pinInput = currentPin + digit, errorMessage = null) }
        }
    }
    
    /**
     * User presses backspace on the PIN pad.
     */
    fun onPinBackspace() {
        _state.update { it.copy(pinInput = it.pinInput.dropLast(1), errorMessage = null) }
    }
    
    /**
     * User clears the PIN input.
     */
    fun onPinClear() {
        _state.update { it.copy(pinInput = "", errorMessage = null) }
    }
    
    /**
     * User submits the PIN.
     * Verifies PIN, then checks if till assignment is needed.
     */
    fun onPinSubmit() {
        val currentState = _state.value
        val employee = currentState.selectedEmployee ?: return
        val pin = currentState.pinInput
        
        if (pin.length < 4) {
            _state.update { it.copy(errorMessage = "PIN must be at least 4 digits") }
            return
        }
        
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            employeeRepository.verifyPin(employee.id, pin)
                .onSuccess { verifiedEmployee ->
                    println("[LoginViewModel] PIN verification SUCCESS for ${employee.fullName}")
                    println("[LoginViewModel] Employee assignedTillId: ${employee.assignedTillId}")
                    // Check if employee has assigned till
                    if (employee.assignedTillId != null && employee.assignedTillId > 0) {
                        // Already has till, complete login
                        println("[LoginViewModel] Employee has till, completing login...")
                        completeLogin(employee, employee.assignedTillId)
                    } else {
                        // Need till assignment
                        println("[LoginViewModel] Employee needs till assignment, loading tills...")
                        loadTillsForAssignment()
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            pinInput = "", // Clear PIN on failure
                            errorMessage = "Invalid PIN. Please try again."
                        )
                    }
                }
        }
    }
    
    /**
     * Load available tills for assignment.
     * Transitions to TILL_ASSIGNMENT stage.
     */
    private fun loadTillsForAssignment() {
        println("[LoginViewModel] loadTillsForAssignment() called")
        scope.launch {
            println("[LoginViewModel] Fetching tills from repository...")
            tillRepository.getTills()
                .onSuccess { tills ->
                    println("[LoginViewModel] SUCCESS: Got ${tills.size} tills")
                    tills.forEach { till ->
                        println("[LoginViewModel]   Till: id=${till.id}, name=${till.name}")
                    }
                    _state.update { 
                        it.copy(
                            tills = tills.map { till -> till.toUiModel() },
                            stage = LoginStage.TILL_ASSIGNMENT,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    println("[LoginViewModel] FAILED to load tills: ${error.message}")
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load tills"
                        )
                    }
                }
        }
    }
    
    /**
     * User selects a till.
     * 
     * Per TILL_MANAGEMENT.md: Till assignment happens as part of the login API call,
     * not as a separate endpoint. The tillId is passed to the login API.
     * 
     * Business Rules:
     * - Cashier can select available (unassigned) tills
     * - Cashier can select their OWN assigned till
     * - Cashier CANNOT select tills assigned to other employees
     */
    fun onTillSelected(tillId: Int) {
        println("[LoginViewModel] onTillSelected: tillId=$tillId")
        
        val till = _state.value.tills.find { it.id == tillId }
        val employee = _state.value.selectedEmployee
        
        if (till == null) {
            println("[LoginViewModel] ERROR: Till not found")
            _state.update { it.copy(errorMessage = "Till not found") }
            return
        }
        
        if (employee == null) {
            println("[LoginViewModel] ERROR: No employee selected")
            return
        }
        
        // Check if till is selectable by this employee
        val isOwnTill = till.assignedEmployeeId == employee.id
        val isSelectable = till.isAvailable || isOwnTill
        
        if (!isSelectable) {
            println("[LoginViewModel] ERROR: Till not available - assigned to ${till.assignedTo}")
            _state.update { it.copy(errorMessage = "This till is already assigned to ${till.assignedTo}") }
            return
        }
        
        println("[LoginViewModel] Till ${till.name} selected for ${employee.fullName} (isOwnTill=$isOwnTill)")
        
        // Per TILL_MANAGEMENT.md: Complete login with selected till
        // The till assignment is handled as part of the login API call
        completeLogin(employee, tillId)
    }
    
    /**
     * Complete the login process.
     * Creates AuthUser and transitions to SUCCESS stage.
     */
    private fun completeLogin(employee: EmployeeUiModel, tillId: Int) {
        // Map role string back to enum
        val role = when (employee.role.lowercase()) {
            "administrator" -> UserRole.ADMIN
            "manager" -> UserRole.MANAGER
            "supervisor" -> UserRole.SUPERVISOR
            else -> UserRole.CASHIER
        }
        
        val authUser = AuthUser(
            id = employee.id.toString(),
            username = employee.fullName,
            role = role,
            permissions = emptyList(), // Will be loaded separately
            isManager = role == UserRole.MANAGER || role == UserRole.SUPERVISOR || role == UserRole.ADMIN,
            jobTitle = employee.role,
            imageUrl = employee.imageUrl
        )
        
        _state.update { 
            it.copy(
                stage = LoginStage.SUCCESS,
                authenticatedUser = authUser,
                selectedTillId = tillId,
                isLoading = false
            )
        }
    }
    
    /**
     * User presses back button.
     * Navigates to previous stage.
     */
    fun onBackPressed() {
        when (_state.value.stage) {
            LoginStage.PIN_ENTRY -> {
                _state.update { 
                    it.copy(
                        stage = LoginStage.EMPLOYEE_SELECT,
                        selectedEmployee = null,
                        pinInput = "",
                        errorMessage = null
                    )
                }
            }
            LoginStage.TILL_ASSIGNMENT -> {
                _state.update { 
                    it.copy(
                        stage = LoginStage.PIN_ENTRY,
                        tills = emptyList(),
                        errorMessage = null
                    )
                }
            }
            else -> { /* No-op */ }
        }
    }
    
    /**
     * Dismiss error message.
     */
    fun onErrorDismissed() {
        _state.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Refresh employee list.
     */
    fun onRefresh() {
        loadEmployees()
    }
    
    // ========================================================================
    // NFC Badge Login
    // ========================================================================
    
    /**
     * Initiates NFC badge scan for login.
     *
     * Per ANDROID_HARDWARE_GUIDE.md:
     * - Opens ScanBadgeDialog
     * - Starts NFC scanner and waits for badge tap
     * - On success, uses badge token as PIN
     *
     * Governance:
     * - Non-Blocking: UI remains responsive during scan
     * - Fallback: User can Cancel and return to PIN entry
     */
    fun onBadgeLoginClick() {
        val employee = _state.value.selectedEmployee ?: return
        
        // Show the scan dialog
        _state.update { it.copy(isNfcScanActive = true, errorMessage = null) }
        
        // Start the scan in background
        scope.launch {
            val result = nfcScanner.startScan()
            
            // Hide the dialog first
            _state.update { it.copy(isNfcScanActive = false) }
            
            when (result) {
                is NfcResult.Success -> {
                    // Badge token is used as PIN
                    // Per spec: Badge "9999" -> PIN "9999"
                    handleNfcLoginSuccess(employee, result.token)
                }
                is NfcResult.Error -> {
                    _state.update { 
                        it.copy(errorMessage = "Badge scan failed. Please try again or use PIN.")
                    }
                }
                is NfcResult.Cancelled -> {
                    // User cancelled, just close dialog (already done above)
                }
            }
        }
    }
    
    /**
     * Cancels an ongoing NFC scan.
     *
     * Called when user presses Cancel button on ScanBadgeDialog.
     */
    fun onCancelNfcScan() {
        nfcScanner.cancelScan()
        _state.update { it.copy(isNfcScanActive = false) }
    }
    
    /**
     * Handles successful NFC badge read.
     *
     * Uses the badge token as PIN for authentication.
     */
    private fun handleNfcLoginSuccess(employee: EmployeeUiModel, token: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Verify the token as PIN
            employeeRepository.verifyPin(employee.id, token)
                .onSuccess { _ ->
                    // Check if employee has assigned till
                    if (employee.assignedTillId != null && employee.assignedTillId > 0) {
                        // Already has till, complete login
                        completeLogin(employee, employee.assignedTillId)
                    } else {
                        // Need till assignment
                        loadTillsForAssignment()
                    }
                }
                .onFailure { _ ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Badge not recognized. Please try again or use PIN."
                        )
                    }
                }
        }
    }
    
    // ========================================================================
    // Admin Settings (Hidden Menu)
    // ========================================================================
    
    /**
     * Show admin settings dialog.
     * Per SCREEN_LAYOUTS.md: Triggered by secret click on copyright text.
     */
    fun showAdminSettings() {
        _state.update { it.copy(showAdminSettings = true) }
    }
    
    /**
     * Hide admin settings dialog.
     */
    fun hideAdminSettings() {
        _state.update { it.copy(showAdminSettings = false) }
    }
    
    /**
     * Get current time formatted for display.
     */
    private fun getCurrentTime(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = if (now.hour == 0) 12 else if (now.hour > 12) now.hour - 12 else now.hour
        val amPm = if (now.hour >= 12) "PM" else "AM"
        return "${hour}:${now.minute.toString().padStart(2, '0')} $amPm"
    }
}
