package dev.devscroll.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BlogDto(
    @SerializedName("articleCount") val articleCount: Int,
    @SerializedName("blogId") val blogId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("rssUrl") val rssUrl: String,
    @SerializedName("url") val url: String,
    @SerializedName("subscribed") val isSubscribed: Boolean = true,
)