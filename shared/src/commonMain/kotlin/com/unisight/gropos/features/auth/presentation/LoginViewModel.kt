package com.unisight.gropos.features.auth.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.features.auth.domain.usecase.ValidateLoginUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScreenModel (ViewModel) for the Login screen.
 * 
 * Uses Voyager's ScreenModel for multiplatform compatibility.
 * 
 * Per kotlin-standards.mdc:
 * - Uses screenModelScope for structured concurrency
 * - Exposes StateFlow for reactive UI updates
 * - Never hardcodes Dispatchers (uses screenModelScope default)
 * 
 * Per project-structure.mdc: Named [Feature]ViewModel
 * 
 * @param validateLogin UseCase for login validation
 * @param coroutineScope Scope for launching coroutines (defaults to screenModelScope, injectable for tests)
 */
class LoginViewModel(
    private val validateLogin: ValidateLoginUseCase,
    private val coroutineScope: CoroutineScope? = null
) : ScreenModel {
    
    /**
     * Returns the coroutine scope to use for launching coroutines.
     * Uses injected scope if provided (for testing), otherwise uses screenModelScope.
     */
    private val scope: CoroutineScope
        get() = coroutineScope ?: screenModelScope
    
    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    
    /**
     * Current login UI state.
     * Observe this in Compose using collectAsState().
     */
    val state: StateFlow<LoginUiState> = _state.asStateFlow()
    
    /**
     * Attempts to log in with the provided credentials.
     * 
     * Flow:
     * 1. Set state to Loading
     * 2. Call ValidateLoginUseCase
     * 3. On success: Set state to Success
     * 4. On failure: Set state to Error with message
     * 
     * @param username The employee username
     * @param pin The PIN (will be validated by UseCase)
     */
    fun onLoginClick(username: String, pin: String) {
        // Don't allow multiple simultaneous login attempts
        if (_state.value is LoginUiState.Loading) return
        
        scope.launch {
            _state.value = LoginUiState.Loading
            
            validateLogin(username, pin)
                .onSuccess { user ->
                    _state.value = LoginUiState.Success(user)
                }
                .onFailure { error ->
                    _state.value = LoginUiState.Error(
                        message = error.message ?: "An unexpected error occurred"
                    )
                }
            
            // Security best practice: Clear PIN from any local state
            // Note: The PIN parameter is a local copy, it will be garbage collected
            // The caller (UI) should also clear its TextField state after submission
        }
    }
    
    /**
     * Resets the state to Idle.
     * Called when user dismisses error or navigates back.
     */
    fun resetState() {
        _state.value = LoginUiState.Idle
    }
    
    /**
     * Clears error state and returns to Idle.
     * Called when user wants to retry after an error.
     */
    fun onErrorDismissed() {
        if (_state.value is LoginUiState.Error) {
            _state.value = LoginUiState.Idle
        }
    }
}
