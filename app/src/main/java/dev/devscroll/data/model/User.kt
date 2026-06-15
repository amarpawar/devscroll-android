package dev.devscroll.data.model

data class User(
    val email: String,
    val name: String,
    val pictureUrl: String?,
    val savedArticlesCount: Int = 0,
    val subscriptionsCount: Int = 0,
    val streakCount: Int = 0,
    val provider: String = "username/password"
)
