package dev.devscroll.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.User
import dev.devscroll.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    object LoggedOut : AuthUiState
    object Loading : AuthUiState
    object Authenticating : AuthUiState
    data class LoggedIn(val user: User, val isRefreshing: Boolean = false) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _localState = MutableStateFlow<AuthUiState?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<AuthUiState> = combine(
        authRepository.userState,
        _localState,
        _isRefreshing
    ) { user, local, refreshing ->
        when {
            local != null -> local
            user != null -> AuthUiState.LoggedIn(user, refreshing)
            else -> AuthUiState.LoggedOut
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState.Loading
    )

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _localState.value = AuthUiState.Authenticating
            authRepository.signInWithGoogle(idToken)
                .onSuccess {
                    _localState.value = null
                }
                .onFailure { error ->
                    _localState.value = AuthUiState.Error(error.message ?: "Google Sign-In failed")
                }
        }
    }

    fun loginWithFacebook(accessToken: String) {
        viewModelScope.launch {
            _localState.value = AuthUiState.Authenticating
            authRepository.signInWithFacebook(accessToken)
                .onSuccess {
                    _localState.value = null
                }
                .onFailure { error ->
                    _localState.value = AuthUiState.Error(error.message ?: "Facebook Sign-In failed")
                }
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _localState.value = AuthUiState.Authenticating
            authRepository.signInWithEmail(email, pass)
                .onSuccess {
                    _localState.value = null
                }
                .onFailure { error ->
                    _localState.value = AuthUiState.Error(error.message ?: "Login failed")
                }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _localState.value = AuthUiState.Authenticating
            authRepository.signUpWithEmail(email, pass)
                .onSuccess {
                    _localState.value = null
                }
                .onFailure { error ->
                    _localState.value = AuthUiState.Error(error.message ?: "Sign-up failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _localState.value = AuthUiState.Authenticating
            authRepository.logout()
                .onSuccess {
                    _localState.value = null
                }
                .onFailure { error ->
                    _localState.value = AuthUiState.Error(error.message ?: "Logout failed")
                }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _isRefreshing.value = true
            authRepository.refreshProfile()
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _localState.value = null
    }
}
