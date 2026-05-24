package dev.devscroll.data.remote

import dev.devscroll.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DevscrollApiService {

    @GET("devscroll/api/articles")
    suspend fun getArticles(
        @Query("page") page: Int,
        @Query("size") size: Int = 20,
        @Query("sortType") sortType: String = "NEWEST",
    ): ArticlesPageDto

    @GET("devscroll/api/articles/search")
    suspend fun searchArticles(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
    ): ArticlesPageDto

    // TODO: confirm endpoint paths with backend
    @POST("devscroll/api/articles/{id}/toggle-save")
    suspend fun toggleSave(@Path("id") articleId: Long)

    @GET("devscroll/api/articles/saved")
    suspend fun getSavedArticles(
        @Query("page") page: Int,
        @Query("size") size: Int = 20,
    ): ArticlesPageDto

    @POST("devscroll/api/auth/register")
    suspend fun registerWithEmail(@Body request: EmailSignUpRequest): TokenPair

    @POST("devscroll/api/auth/login")
    suspend fun loginWithEmail(@Body request: EmailSignInRequest): TokenPair

    @POST("devscroll/api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleSignInRequest): TokenPair

    @POST("devscroll/api/auth/facebook")
    suspend fun loginWithFacebook(@Body request: FacebookSignInRequest): TokenPair

    @POST("devscroll/api/auth/refresh")
    suspend fun refreshSession(@Body request: RefreshRequest): TokenPair

    @POST("devscroll/api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    @POST("devscroll/api/auth/logout-all")
    suspend fun logoutAll(): Response<Unit>

    @GET("devscroll/api/users/me")
    suspend fun getUserProfile(): UserProfileDto
}
