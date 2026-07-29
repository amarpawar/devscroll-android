package dev.devscroll.data.repository

import dev.devscroll.data.local.AppDatabase
import dev.devscroll.data.local.AuthPreferences
import dev.devscroll.data.model.User
import dev.devscroll.data.remote.DevscrollApiService
import dev.devscroll.data.remote.dto.*
import dev.devscroll.util.CrashReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/** The server rejected the re-authentication credential (wrong password, stale token). */
class InvalidCredentialException : Exception("Incorrect credentials — account not deleted")

/** Carries a message the server wrote for the user, safe to display as-is. */
class AuthFailureException(message: String) : Exception(message)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: DevscrollApiService,
    private val authPreferences: AuthPreferences,
    private val database: AppDatabase,
    private val crashReporter: CrashReporter,
) : AuthRepository {

    override val userState: Flow<User?> = authPreferences.user

    override suspend fun signInWithGoogle(idToken: String): Result<User> = authCall("google sign-in") {
        val tokens = apiService.loginWithGoogle(GoogleSignInRequest(idToken))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("google")
    }

    override suspend fun signInWithFacebook(accessToken: String): Result<User> = authCall("facebook sign-in") {
        val tokens = apiService.loginWithFacebook(FacebookSignInRequest(accessToken))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("facebook")
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<User> = authCall("email sign-in") {
        val tokens = apiService.loginWithEmail(EmailSignInRequest(email, pass))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("username/password")
    }

    override suspend fun signUpWithEmail(email: String, pass: String): Result<User> = authCall("email sign-up") {
        val tokens = apiService.registerWithEmail(EmailSignUpRequest(email, pass))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("username/password")
    }

    /**
     * Runs an auth call, replacing Retrofit's bare `"HTTP 400 Bad Request"` with the
     * server's own ProblemDetail message, and reporting anything that isn't the user's
     * fault so it shows up in Crashlytics rather than only on their screen.
     */
    private suspend fun <T> authCall(operation: String, block: suspend () -> T): Result<T> =
        runCatching { block() }.recoverCatching { error ->
            // runCatching swallows cancellation; letting it through would report a
            // non-fatal every time the user leaves the screen mid-request.
            if (error is CancellationException) throw error
            if (!error.isExpectedClientError()) {
                crashReporter.recordNonFatal(error, "Auth failed: $operation")
            }
            throw error.userFacingMessage()?.let(::AuthFailureException) ?: error
        }

    override suspend fun forgotPassword(email: String): Result<Unit> =
        authCall("forgot password") {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email))
            if (!response.isSuccessful) {
                throw IllegalStateException("Request failed (${response.code()})")
            }
        }

    override suspend fun logout(): Result<Unit> = runCatching {
        val currentRefreshToken = authPreferences.refreshToken.first()
        if (!currentRefreshToken.isNullOrBlank()) {
            runCatching {
                apiService.logout(LogoutRequest(currentRefreshToken))
            }
        }
        authPreferences.clearSession()
    }

    private suspend fun fetchAndSaveProfile(provider: String): User {
        val profile = apiService.getUserProfile()
        val saved = profile.savedArticlesCount ?: 0
        val subs = profile.subscriptionsCount ?: 0
        val streak = profile.streakCount ?: 0
        authPreferences.saveProfile(
            email = profile.email,
            name = profile.name,
            pictureUrl = profile.pictureUrl,
            savedArticlesCount = saved,
            subscriptionsCount = subs,
            streakCount = streak,
            provider = provider
        )
        return User(
            email = profile.email,
            name = profile.name,
            pictureUrl = profile.pictureUrl,
            savedArticlesCount = saved,
            subscriptionsCount = subs,
            streakCount = streak,
            provider = provider
        )
    }

    override suspend fun refreshProfile(): Result<User> = runCatching {
        val currentUser = userState.first() ?: throw IllegalStateException("User not logged in")
        fetchAndSaveProfile(currentUser.provider)
    }

    override suspend fun deleteAccount(request: DeleteAccountRequest): Result<Unit> = runCatching {
        val response = apiService.deleteAccount(request)
        if (!response.isSuccessful) {
            // 401/403 means the server rejected the reauth credential — a different
            // problem for the user than a server or network fault, so name it.
            throw if (response.code() == 401 || response.code() == 403) {
                InvalidCredentialException()
            } else {
                IllegalStateException("Delete failed (${response.code()})")
            }
        }
        // Only once the server has confirmed: saved articles are user data too, so the
        // local cache goes with the account.
        authPreferences.clearSession()
        // clearAllTables() is blocking, unlike the suspend DAO methods, and this
        // coroutine resumes on the main dispatcher after the network call.
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    override suspend fun exportUserData(): Result<ResponseBody> = runCatching {
        apiService.exportUserData()
    }
}
