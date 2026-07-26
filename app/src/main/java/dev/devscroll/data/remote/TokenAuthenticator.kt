package dev.devscroll.data.remote

import dev.devscroll.data.local.AuthPreferences
import dev.devscroll.data.remote.dto.RefreshRequest
import dev.devscroll.util.CrashReporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Route
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val authApiService: AuthApiService,
    private val crashReporter: CrashReporter,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // One refresh attempt per request. Without this, a server that keeps
        // answering TOKEN_EXPIRED would drive an unbounded retry chain.
        if (responseCount(response) > MAX_ATTEMPTS) {
            return null
        }

        val bodyString = runCatching { response.peekBody(PEEK_BYTES).string() }.getOrNull()
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
                    // Isolated client (NetworkModule.provideAuthOkHttpClient): this call
                    // carries no bearer token and cannot re-enter this authenticator,
                    // which would otherwise deadlock on the mutex we already hold.
                    val newPair = authApiService.refreshSession(RefreshRequest(currentRefreshToken))

                    // Store new rotated tokens
                    authPreferences.saveTokens(newPair)

                    // Rebuild the request with new token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newPair.accessToken}")
                        .build()
                } catch (e: Exception) {
                    // Refresh token is expired or revoked. Force logout.
                    // Worth counting: a spike here means users are being signed out.
                    crashReporter.recordNonFatal(e, "Token refresh failed — forcing logout")
                    authPreferences.clearSession()
                    null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val PEEK_BYTES = 1024L
    }
}
