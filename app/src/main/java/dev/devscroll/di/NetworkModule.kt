package dev.devscroll.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.devscroll.BuildConfig
import dev.devscroll.data.remote.AuthApiService
import dev.devscroll.data.remote.AuthInterceptor
import dev.devscroll.data.remote.TokenAuthenticator
import dev.devscroll.data.remote.DevscrollApiService
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * BODY level writes login responses — including refresh tokens — straight to
     * logcat, so debug builds stay at BASIC with the bearer header redacted.
     */
    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    private fun OkHttpClient.Builder.withTimeouts() = apply {
        connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool = ConnectionPool()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        connectionPool: ConnectionPool,
    ): OkHttpClient = OkHttpClient.Builder()
        .withTimeouts()
        .connectionPool(connectionPool)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): DevscrollApiService =
        retrofit.create(DevscrollApiService::class.java)

    /**
     * The token-refresh stack. Built from scratch rather than derived from the main
     * client so it carries neither [AuthInterceptor] nor [TokenAuthenticator]: a
     * refresh must never attach a stale bearer token, because a 401 on the refresh
     * itself would re-enter the authenticator on the same thread while its mutex is
     * held and block that thread permanently. Shares the connection pool so the
     * isolation costs no extra sockets.
     */
    @Provides
    @Singleton
    @AuthClient
    fun provideAuthOkHttpClient(connectionPool: ConnectionPool): OkHttpClient =
        OkHttpClient.Builder()
            .withTimeouts()
            .connectionPool(connectionPool)
            .addInterceptor(loggingInterceptor())
            .build()

    @Provides
    @Singleton
    @AuthClient
    fun provideAuthRetrofit(@AuthClient okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthApiService(@AuthClient retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)
}
