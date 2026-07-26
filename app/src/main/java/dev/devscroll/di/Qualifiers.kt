package dev.devscroll.di

import javax.inject.Qualifier

/** Application-lifetime coroutine scope, outliving any Activity or ViewModel. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * The bare networking stack used for token refresh. It deliberately has no
 * [dev.devscroll.data.remote.AuthInterceptor] and no authenticator, so a refresh
 * call can never re-enter [dev.devscroll.data.remote.TokenAuthenticator].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient
