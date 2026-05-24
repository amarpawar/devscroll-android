package dev.devscroll.data.remote

import dev.devscroll.data.remote.dto.ArticlesPageDto
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
}
