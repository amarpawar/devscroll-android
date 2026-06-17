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

    @GET("devscroll/api/blog")
    suspend fun getBlogs(): List<BlogDto>

    @GET("devscroll/api/articles/search")
    suspend fun searchArticles(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
    ): ArticlesPageDto

    @POST("devscroll/api/users/me/saved-articles/{article_id}")
    suspend fun saveArticle(@Path("article_id") articleId: Long)

    @DELETE("devscroll/api/users/me/saved-articles/{article_id}")
    suspend fun unsaveArticle(@Path("article_id") articleId: Long)

    @POST("devscroll/api/articles/{article_id}/hit")
    suspend fun recordArticleHit(@Path("article_id") articleId: Long)

    @GET("devscroll/api/users/me/saved-articles")
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

    @POST("devscroll/api/users/me/unsubscriptions/{blogId}")
    suspend fun unsubscribeBlog(@Path("blogId") blogId: Long)

    @DELETE("devscroll/api/users/me/unsubscriptions/{blogId}")
    suspend fun subscribeBlog(@Path("blogId") blogId: Long)
}
