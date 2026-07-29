package dev.devscroll.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.devscroll.data.model.User
import dev.devscroll.data.remote.dto.DeleteAccountRequest
import dev.devscroll.data.repository.AuthRepository
import dev.devscroll.data.repository.InvalidCredentialException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Kept separate from [AuthUiState] so requesting a reset doesn't disturb the sign-in form. */
sealed interface ForgotPasswordState {
    object Idle : ForgotPasswordState
    object Sending : ForgotPasswordState
    object Sent : ForgotPasswordState
    data class Error(val message: String) : ForgotPasswordState
}

sealed interface AuthUiState {
    object LoggedOut : AuthUiState
    object Loading : AuthUiState
    object Authenticating : AuthUiState
    data class LoggedIn(val user: User, val isRefreshing: Boolean = false) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val contentResolver get() = appContext.contentResolver

    private val _localState = MutableStateFlow<AuthUiState?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

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

    fun forgotPassword(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Enter your email address")
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState.Sending
            authRepository.forgotPassword(trimmed)
                .onSuccess { _forgotPasswordState.value = ForgotPasswordState.Sent }
                .onFailure { error ->
                    _forgotPasswordState.value =
                        ForgotPasswordState.Error(error.message ?: "Could not send reset link")
                }
        }
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }

    /** Surfaces failures raised by the credential UIs before a repository call happens. */
    fun onAuthError(message: String) {
        _localState.value = AuthUiState.Error(message)
    }

    /**
     * Deletes the account after the server has verified [request]'s fresh credential.
     * On failure nothing is destroyed — the user stays signed in.
     */
    fun deleteAccount(request: DeleteAccountRequest) {
        viewModelScope.launch {
            _localState.value = AuthUiState.Authenticating
            authRepository.deleteAccount(request)
                .onSuccess {
                    // userState goes null on its own once the session is cleared.
                    _localState.value = null
                }
                .onFailure { error ->
                    _localState.value = AuthUiState.Error(
                        when (error) {
                            is InvalidCredentialException -> error.message!!
                            else -> error.message ?: "Could not delete account"
                        }
                    )
                }
        }
    }

    fun exportUserData(destination: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            val result = authRepository.exportUserData().mapCatching { body ->
                // Streamed rather than buffered: the export can be arbitrarily large.
                withContext(Dispatchers.IO) {
                    body.byteStream().use { input ->
                        contentResolver.openOutputStream(destination)?.use { output ->
                            input.copyTo(output)
                        } ?: throw IllegalStateException("Could not open ${destination.lastPathSegment}")
                    }
                }
            }
            _isExporting.value = false
            result
                .onSuccess { _exportMessage.value = "export saved" }
                .onFailure { _localState.value = AuthUiState.Error(it.message ?: "Export failed") }
        }
    }

    fun clearExportMessage() { _exportMessage.value = null }

    fun clearError() {
        _localState.value = null
    }
}
