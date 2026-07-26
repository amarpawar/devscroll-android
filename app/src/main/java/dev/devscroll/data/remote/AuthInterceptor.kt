package dev.devscroll.data.remote

import dev.devscroll.data.local.AuthPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // In-memory mirror; reading DataStore here blocked the OkHttp thread on disk
        // for every request.
        val token = authPreferences.cachedToken()
        val builder = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
