package dev.devscroll.ui.profile

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.devscroll.R

/**
 * Runs the Google credential sheet and returns a fresh ID token.
 *
 * Shared by first-time sign-in and by the re-authentication step of account deletion,
 * which needs a *freshly minted* token rather than the stored session.
 */
suspend fun requestGoogleIdToken(context: Context): String {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(context.getString(R.string.google_web_client_id))
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val result = CredentialManager.create(context).getCredential(context = context, request = request)
    val credential = result.credential
    require(
        credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) { "Unexpected credential type" }

    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
