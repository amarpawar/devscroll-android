package dev.devscroll.data.remote

import dev.devscroll.data.remote.dto.RefreshRequest
import dev.devscroll.data.remote.dto.TokenPair
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Token refresh, served by its own bare OkHttp client (see [dev.devscroll.di.AuthClient]).
 *
 * Keeping this off the main client means the refresh call carries no Authorization
 * header and cannot trigger [TokenAuthenticator], which would otherwise re-enter the
 * authenticator on the same thread while its mutex is held and deadlock it.
 */
interface AuthApiService {

    @POST("devscroll/api/auth/refresh")
    suspend fun refreshSession(@Body request: RefreshRequest): TokenPair
}
