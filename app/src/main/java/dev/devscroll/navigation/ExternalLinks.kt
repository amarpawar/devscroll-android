package dev.devscroll.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Hosted pages the app links out to.
 *
 * Both must be live before Play submission — a privacy policy URL that 404s is a
 * rejection, and the sign-up footer claims the user has agreed to them.
 */
object ExternalLinks {
    const val PRIVACY = "https://devscroll.in/privacy"
    const val TERMS = "https://devscroll.in/terms"
}

/** Opens [url] in the user's browser, matching the reader's "open in browser" behaviour. */
fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
    }
}
