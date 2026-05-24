package dev.devscroll.data.model

data class User(
    val email: String,
    val name: String,
    val pictureUrl: String?,
    val savedCount: Int = 0,
    val subsCount: Int = 0,
    val streakCount: Int = 0,
    val provider: String = "username/password"
)
