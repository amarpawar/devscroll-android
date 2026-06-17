package dev.devscroll.data.remote

import dev.devscroll.data.local.AuthPreferences
import dev.devscroll.data.remote.dto.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Route
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val apiServiceProvider: Provider<DevscrollApiService>
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        val bodyString = runCatching { response.peekBody(1024).string() }.getOrNull()
        val isTokenExpired = bodyString != null && bodyString.contains("TOKEN_EXPIRED")
        if (!isTokenExpired) {
            return null
        }

        return runBlocking {
            mutex.withLock {
                val currentToken = authPreferences.accessToken.first()

                // If another request already refreshed the token in parallel, retry immediately with that
                if (response.request.header("Authorization") != "Bearer $currentToken") {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val currentRefreshToken = authPreferences.refreshToken.first() ?: return@withLock null

                try {
                    // Make synchronous refresh call
                    val apiService = apiServiceProvider.get()
                    val newPair = apiService.refreshSession(RefreshRequest(currentRefreshToken))

                    // Store new rotated tokens
                    authPreferences.saveTokens(newPair)

                    // Rebuild the request with new token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newPair.accessToken}")
                        .build()
                } catch (e: Exception) {
                    // Refresh token is expired or revoked. Force logout.
                    authPreferences.clearSession()
                    null
                }
            }
        }
    }
}
