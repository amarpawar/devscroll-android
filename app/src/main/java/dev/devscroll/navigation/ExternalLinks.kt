package dev.devscroll.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Hosted pages the app links out to.
 *
 * Served by the backend's LegalController under the `/devscroll/api` context path, and
 * whitelisted in SecurityConfig so they load unauthenticated — which is what Play review
 * requires.
 *
 * Pinned to production rather than derived from [dev.devscroll.BuildConfig.API_BASE_URL]:
 * the documents are identical in every environment, and a `local` build would otherwise
 * point a tester at an unreachable LAN address.
 */
object ExternalLinks {
    private const val LEGAL_BASE = "https://api.devscroll.in/devscroll/api/legal"

    const val PRIVACY = "$LEGAL_BASE/privacy"
    const val TERMS = "$LEGAL_BASE/terms"
}

/** Opens [url] in the user's browser, matching the reader's "open in browser" behaviour. */
fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
    }
}
