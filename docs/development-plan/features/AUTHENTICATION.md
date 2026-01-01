# Authentication

This document covers the authentication flow in GroPOS, including login, logout, and device lock functionality.

## Overview

GroPOS uses a two-tier authentication model:
1. **Device Authentication**: API key for device-level operations
2. **Employee Authentication**: Bearer token for user-level operations

## Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Authentication Flow                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                   │
│  │  Device     │────▶│  Employee   │────▶│   Active    │                   │
│  │  Registered │     │   Login     │     │   Session   │                   │
│  └─────────────┘     └─────────────┘     └──────┬──────┘                   │
│                                                  │                          │
│                           ┌──────────────────────┼──────────────────────┐   │
│                           │                      │                      │   │
│                           ▼                      ▼                      ▼   │
│                    ┌─────────────┐        ┌─────────────┐        ┌─────────┐│
│                    │  Inactivity │        │   Manual    │        │  Logout ││
│                    │    Lock     │        │    Lock     │        │         ││
│                    └──────┬──────┘        └──────┬──────┘        └────┬────┘│
│                           │                      │                    │     │
│                           └──────────┬───────────┘                    │     │
│                                      ▼                                │     │
│                               ┌─────────────┐                         │     │
│                               │  Lock View  │                         │     │
│                               └──────┬──────┘                         │     │
│                                      │                                │     │
│                           ┌──────────┴──────────┐                     │     │
│                           ▼                     ▼                     │     │
│                    ┌─────────────┐       ┌─────────────┐             │     │
│                    │   Unlock    │       │   Logout    │◀────────────┘     │
│                    │  (PIN)      │       │  (Clear)    │                   │
│                    └──────┬──────┘       └──────┬──────┘                   │
│                           │                     │                          │
│                           ▼                     ▼                          │
│                    ┌─────────────┐       ┌─────────────┐                   │
│                    │   Resume    │       │   Login     │                   │
│                    │   Session   │       │   Screen    │                   │
│                    └─────────────┘       └─────────────┘                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Login Flow

### LoginScreen (Compose)

```kotlin
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "GroPOS Logo"
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Branch Selector
        BranchDropdown(
            branches = uiState.branches,
            selectedBranch = uiState.selectedBranch,
            onBranchSelected = { viewModel.selectBranch(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // PIN Input
        OutlinedTextField(
            value = uiState.pin,
            onValueChange = { viewModel.updatePin(it) },
            label = { Text("Enter PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Login Button
        Button(
            onClick = { viewModel.login() },
            enabled = uiState.canLogin && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colors.onPrimary
                )
            } else {
                Text("Login")
            }
        }
    }
    
    // Error Dialog
    uiState.error?.let { error ->
        ErrorDialog(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }
    
    // Success Navigation
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess()
        }
    }
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appStore: AppStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    init {
        loadBranches()
    }
    
    private fun loadBranches() {
        viewModelScope.launch {
            try {
                val branches = authRepository.getBranches()
                _uiState.update { it.copy(branches = branches) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load branches") }
            }
        }
    }
    
    fun selectBranch(branch: BranchDto) {
        _uiState.update { it.copy(selectedBranch = branch) }
    }
    
    fun updatePin(pin: String) {
        _uiState.update { it.copy(pin = pin) }
    }
    
    fun login() {
        val branch = _uiState.value.selectedBranch ?: return
        val pin = _uiState.value.pin
        
        if (pin.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter your PIN") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Call login API
                val token = authRepository.login(
                    pin = pin,
                    branchId = branch.id
                )
                
                // Save tokens
                authRepository.saveTokens(token)
                
                // Load user profile
                val profile = authRepository.getUserProfile()
                appStore.setEmployee(profile)
                appStore.setBranch(branch)
                
                // Load initial data if needed
                if (authRepository.isFirstLogin()) {
                    authRepository.loadInitialData()
                }
                
                _uiState.update { it.copy(loginSuccess = true, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Login failed: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }
}
```

## Screen Lock

### Inactivity Timer

```kotlin
class InactivityManager(
    private val scope: CoroutineScope,
    private val authRepository: AuthRepository,
    private val onLock: () -> Unit
) {
    companion object {
        private const val INACTIVITY_LIMIT = 5 * 60 * 1000L // 5 minutes
    }
    
    private var lockJob: Job? = null
    
    fun startTimer() {
        lockJob?.cancel()
        lockJob = scope.launch {
            delay(INACTIVITY_LIMIT)
            performLock(DeviceEventType.AutoLocked)
        }
    }
    
    fun resetTimer() {
        startTimer()
    }
    
    fun manualLock() {
        lockJob?.cancel()
        scope.launch {
            performLock(DeviceEventType.Locked)
        }
    }
    
    private suspend fun performLock(lockType: DeviceEventType) {
        try {
            authRepository.lockDevice(lockType)
        } catch (e: Exception) {
            // Handle with token refresh if needed
        }
        withContext(Dispatchers.Main) {
            onLock()
        }
    }
    
    fun stop() {
        lockJob?.cancel()
    }
}
```

### LockScreen (Compose)

```kotlin
@Composable
fun LockScreen(
    viewModel: LockViewModel = hiltViewModel(),
    onUnlock: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Employee Name
        Text(
            text = uiState.employeeName,
            style = MaterialTheme.typography.h5
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Device Locked",
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.secondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN Input
        OutlinedTextField(
            value = uiState.pin,
            onValueChange = { viewModel.updatePin(it) },
            label = { Text("Enter PIN to Unlock") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.error != null,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onLogout) {
                Text("Logout")
            }
            
            Button(
                onClick = { viewModel.unlock() },
                enabled = !uiState.isLoading
            ) {
                Text("Unlock")
            }
        }
    }
    
    // Success Navigation
    LaunchedEffect(uiState.unlockSuccess) {
        if (uiState.unlockSuccess) {
            onUnlock()
        }
    }
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appStore: AppStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LockUiState(
        employeeName = appStore.employee.value?.fullName ?: ""
    ))
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()
    
    fun updatePin(pin: String) {
        _uiState.update { it.copy(pin = pin, error = null) }
    }
    
    fun unlock() {
        val pin = _uiState.value.pin
        if (pin.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter your PIN") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                authRepository.unlock(pin)
                _uiState.update { it.copy(unlockSuccess = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Invalid PIN",
                    isLoading = false
                )}
            }
        }
    }
}
```

## Token Management

### AuthRepository

```kotlin
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage
) {
    suspend fun login(pin: String, branchId: Int): TokenViewModel {
        val request = LoginRequest(pin = pin, branch = branchId)
        val token = apiClient.employeeApi.login(request)
        
        // Store refresh token
        tokenStorage.saveRefreshToken(token.refreshToken)
        
        // Set bearer token for subsequent requests
        apiClient.setBearerToken(token.accessToken)
        
        return token
    }
    
    suspend fun refreshToken(): TokenViewModel? {
        val refreshToken = tokenStorage.getRefreshToken() ?: return null
        
        return try {
            val request = RefreshTokenRequest(
                token = refreshToken,
                clientName = "device"
            )
            
            val newToken = apiClient.employeeApi.refreshToken(request)
            
            tokenStorage.saveRefreshToken(newToken.refreshToken)
            apiClient.setBearerToken(newToken.accessToken)
            
            newToken
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun unlock(pin: String) {
        val request = UnlockRequest(pin = pin)
        apiClient.employeeApi.unlock(request)
    }
    
    suspend fun lockDevice(lockType: DeviceEventType) {
        val request = DeviceLockRequest(lockType = lockType)
        apiClient.employeeApi.lockDevice(request)
    }
    
    fun logout() {
        tokenStorage.clearTokens()
        apiClient.clearBearerToken()
    }
}
```

## Security Considerations

### PIN Requirements
- Numeric only
- Minimum 4 digits
- Stored server-side (never locally)

### Token Storage
- Refresh token stored in secure local storage
- Access token held in memory only
- Tokens scoped to environment (Dev/Staging/Prod)

### Session Security
- Automatic lock after 5 minutes inactivity
- Manual lock available (F4 keyboard shortcut)
- Lock prevents transaction modification
- Logout clears all sensitive data

## API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/employee/login` | POST | Authenticate employee |
| `/employee/profile` | GET | Get user profile |
| `/employee/lock` | POST | Report device lock |
| `/employee/unlock` | POST | Validate unlock PIN |
| `/employee/refresh` | POST | Refresh access token |
| `/employee/logout` | POST | End session |

## Data Models

```kotlin
data class LoginUiState(
    val branches: List<BranchDto> = emptyList(),
    val selectedBranch: BranchDto? = null,
    val pin: String = "",
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null
) {
    val canLogin: Boolean
        get() = selectedBranch != null && pin.length >= 4
}

data class LockUiState(
    val employeeName: String = "",
    val pin: String = "",
    val isLoading: Boolean = false,
    val unlockSuccess: Boolean = false,
    val error: String? = null
)

enum class DeviceEventType {
    Locked,      // Manual lock
    AutoLocked,  // Inactivity lock
    Unlocked     // Device unlocked
}
```

## Related Documentation

- [API Integration](../architecture/API_INTEGRATION.md)
- [State Management](../architecture/STATE_MANAGEMENT.md)

