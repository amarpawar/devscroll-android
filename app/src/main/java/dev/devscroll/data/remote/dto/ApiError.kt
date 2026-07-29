package dev.devscroll.data.remote.dto

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException

/**
 * RFC 7807 `ProblemDetail`, as returned by the backend's exception handlers.
 *
 * Without parsing this, a failed call surfaces only Retrofit's `"HTTP 400 Bad Request"`,
 * which tells the user nothing about what to fix.
 */
data class ApiError(
    @SerializedName("title") val title: String? = null,
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("status") val status: Int? = null,
    /** Per-field validation messages, e.g. {"password": "must be at least 8 characters"}. */
    @SerializedName("errors") val errors: Map<String, String>? = null,
)

/**
 * A message worth showing the user, or null if this isn't an HTTP error we can explain.
 *
 * Field-level validation messages win over the generic detail, since "Validation failed"
 * is no more useful than the status code it replaced.
 */
fun Throwable.userFacingMessage(): String? {
    val httpException = this as? HttpException ?: return null
    val problem = httpException.parseProblemDetail()

    problem?.errors?.entries?.firstOrNull()?.let { (field, message) ->
        return "$field: $message"
    }
    problem?.detail?.takeIf { it.isNotBlank() }?.let { return it }

    // Fall back to something human when the body is missing or unparseable.
    return when (httpException.code()) {
        401, 403 -> "Incorrect email or password"
        409 -> "That email is already registered"
        in 500..599 -> "Server error — please try again"
        else -> null
    }
}

/** True for failures the user caused; these are expected and not worth reporting. */
fun Throwable.isExpectedClientError(): Boolean {
    val code = (this as? HttpException)?.code() ?: return false
    return code in 400..499
}

private fun HttpException.parseProblemDetail(): ApiError? = runCatching {
    // errorBody() is single-shot, but Retrofit has already buffered it by this point.
    response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let {
        Gson().fromJson(it, ApiError::class.java)
    }
}.getOrNull()
