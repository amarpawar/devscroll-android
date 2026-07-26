package dev.devscroll.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.devscroll.data.model.User
import dev.devscroll.data.remote.dto.TokenPair
import dev.devscroll.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userPictureKey = stringPreferencesKey("user_picture")
    private val userSavedArticlesCountKey = androidx.datastore.preferences.core.intPreferencesKey("user_saved_articles_count")
    private val userSubscriptionsCountKey = androidx.datastore.preferences.core.intPreferencesKey("user_subscriptions_count")
    private val userStreakCountKey = androidx.datastore.preferences.core.intPreferencesKey("user_streak_count")
    private val userProviderKey = stringPreferencesKey("user_provider")

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[accessTokenKey]
    }

    /**
     * Mirror of [accessToken] readable without suspending. AuthInterceptor runs on an
     * OkHttp thread for every request; reading DataStore there meant a disk read plus
     * proto deserialization per call.
     */
    @Volatile
    private var cachedAccessToken: String? = null

    fun cachedToken(): String? = cachedAccessToken

    init {
        accessToken
            .onEach { cachedAccessToken = it }
            .launchIn(scope)
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[refreshTokenKey]
    }

    val user: Flow<User?> = context.dataStore.data.map { prefs ->
        val email = prefs[userEmailKey]
        val name = prefs[userNameKey]
        val pictureUrl = prefs[userPictureKey]
        val saved = prefs[userSavedArticlesCountKey] ?: 0
        val subs = prefs[userSubscriptionsCountKey] ?: 0
        val streak = prefs[userStreakCountKey] ?: 0
        val provider = prefs[userProviderKey] ?: "username/password"
        if (email != null && name != null) {
            User(
                email = email,
                name = name,
                pictureUrl = pictureUrl?.ifEmpty { null },
                savedArticlesCount = saved,
                subscriptionsCount = subs,
                streakCount = streak,
                provider = provider
            )
        } else {
            null
        }
    }

    suspend fun saveTokens(tokens: TokenPair) {
        // Set eagerly so a request issued before the flow round-trips uses the new token.
        cachedAccessToken = tokens.accessToken
        context.dataStore.edit { prefs ->
            prefs[accessTokenKey] = tokens.accessToken
            prefs[refreshTokenKey] = tokens.refreshToken
        }
    }

    suspend fun saveProfile(
        email: String,
        name: String,
        pictureUrl: String?,
        savedArticlesCount: Int = 0,
        subscriptionsCount: Int = 0,
        streakCount: Int = 0,
        provider: String = "username/password"
    ) {
        context.dataStore.edit { prefs ->
            prefs[userEmailKey] = email
            prefs[userNameKey] = name
            prefs[userPictureKey] = pictureUrl ?: ""
            prefs[userSavedArticlesCountKey] = savedArticlesCount
            prefs[userSubscriptionsCountKey] = subscriptionsCount
            prefs[userStreakCountKey] = streakCount
            prefs[userProviderKey] = provider
        }
    }

    suspend fun clearSession() {
        cachedAccessToken = null
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
