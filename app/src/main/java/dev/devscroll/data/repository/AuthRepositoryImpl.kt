package dev.devscroll.data.repository

import dev.devscroll.data.local.AuthPreferences
import dev.devscroll.data.model.User
import dev.devscroll.data.remote.DevscrollApiService
import dev.devscroll.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: DevscrollApiService,
    private val authPreferences: AuthPreferences
) : AuthRepository {

    override val userState: Flow<User?> = authPreferences.user

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        val tokens = apiService.loginWithGoogle(GoogleSignInRequest(idToken))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("google")
    }

    override suspend fun signInWithFacebook(accessToken: String): Result<User> = runCatching {
        val tokens = apiService.loginWithFacebook(FacebookSignInRequest(accessToken))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("facebook")
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<User> = runCatching {
        val tokens = apiService.loginWithEmail(EmailSignInRequest(email, pass))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("username/password")
    }

    override suspend fun signUpWithEmail(email: String, pass: String): Result<User> = runCatching {
        val tokens = apiService.registerWithEmail(EmailSignUpRequest(email, pass))
        authPreferences.saveTokens(tokens)
        fetchAndSaveProfile("username/password")
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
}
