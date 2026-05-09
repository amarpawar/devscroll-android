package dev.devscroll.data.remote

import dev.devscroll.data.remote.dto.ArticlesPageDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DevscrollApiService {
    @GET("devscroll/api/articles")
    suspend fun getArticles(
        @Query("page") page: Int,
        @Query("size") size: Int = 20,
        @Query("sortType") sortType: String = "NEWEST",
    ): ArticlesPageDto
}
