package dev.devscroll.data.repository

import dev.devscroll.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val userState: Flow<User?>
    
    suspend fun signInWithGoogle(idToken: String): Result<User>
    
    suspend fun signInWithFacebook(accessToken: String): Result<User>
    
    suspend fun signInWithEmail(email: String, pass: String): Result<User>
    
    suspend fun signUpWithEmail(email: String, pass: String): Result<User>
    
    suspend fun logout(): Result<Unit>
}
