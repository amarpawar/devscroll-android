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

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

/**
 * Carries a freshly obtained credential so the server can verify the user really is
 * present before an irreversible delete. Exactly one field is populated, chosen by the
 * provider the account was created with.
 */
data class DeleteAccountRequest(
    @SerializedName("password") val password: String? = null,
    @SerializedName("idToken") val idToken: String? = null,
    @SerializedName("accessToken") val accessToken: String? = null,
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
    @SerializedName("currentStreak") val streakCount: Int? = 0
)
