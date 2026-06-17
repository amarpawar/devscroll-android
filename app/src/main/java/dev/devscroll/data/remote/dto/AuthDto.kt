package dev.devscroll.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GoogleSignInRequest(
    @SerializedName("idToken") val idToken: String
)

data class FacebookSignInRequest(
    @SerializedName("accessToken") val accessToken: String
)

data class EmailSignInRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class EmailSignUpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class LogoutRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class TokenPair(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Int? = 0
)

data class UserProfileDto(
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("profilePicUrl") val pictureUrl: String?,
    @SerializedName("savedArticlesCount") val savedArticlesCount: Int? = 0,
    @SerializedName("subscriptionsCount") val subscriptionsCount: Int? = 0,
    @SerializedName("streakCount") val streakCount: Int? = 0
)
