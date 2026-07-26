package dev.devscroll.ui.profile

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.devscroll.R
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Raised when the user backs out of a credential sheet — not an error worth surfacing. */
class AuthCancelledException : Exception("Cancelled")

/** Unwraps the ContextWrapper chain Compose hands us to reach the hosting activity. */
tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

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

/**
 * Runs Facebook Login and returns a fresh user access token.
 *
 * Registers a one-shot callback for the duration of the call so it composes as a
 * suspending function; the caller doesn't have to manage callback lifetimes.
 */
suspend fun requestFacebookAccessToken(
    activity: ComponentActivity,
    callbackManager: CallbackManager,
): String = suspendCancellableCoroutine { continuation ->
    val loginManager = LoginManager.getInstance()
    val callback = object : FacebookCallback<LoginResult> {
        override fun onSuccess(result: LoginResult) {
            loginManager.unregisterCallback(callbackManager)
            continuation.resume(result.accessToken.token)
        }

        override fun onCancel() {
            loginManager.unregisterCallback(callbackManager)
            continuation.resumeWithException(AuthCancelledException())
        }

        override fun onError(error: FacebookException) {
            loginManager.unregisterCallback(callbackManager)
            continuation.resumeWithException(error)
        }
    }
    loginManager.registerCallback(callbackManager, callback)
    continuation.invokeOnCancellation { loginManager.unregisterCallback(callbackManager) }
    loginManager.logInWithReadPermissions(activity, callbackManager, listOf("email", "public_profile"))
}
