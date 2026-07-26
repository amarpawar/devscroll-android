package dev.devscroll.data.repository

import dev.devscroll.data.model.User
import dev.devscroll.data.remote.dto.DeleteAccountRequest
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody

interface AuthRepository {
    val userState: Flow<User?>
    
    suspend fun signInWithGoogle(idToken: String): Result<User>
    
    suspend fun signInWithFacebook(accessToken: String): Result<User>
    
    suspend fun signInWithEmail(email: String, pass: String): Result<User>
    
    suspend fun signUpWithEmail(email: String, pass: String): Result<User>
    
    suspend fun logout(): Result<Unit>

    suspend fun refreshProfile(): Result<User>

    /**
     * Irreversibly deletes the account. [request] carries a freshly obtained credential
     * that the server verifies first; on any failure nothing local is touched.
     */
    suspend fun deleteAccount(request: DeleteAccountRequest): Result<Unit>

    suspend fun exportUserData(): Result<ResponseBody>
}
