package dev.devscroll.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Hosted pages the app links out to.
 *
 * Served publicly from the marketing site, so they load unauthenticated — which is what
 * Play review requires.
 *
 * Pinned to production rather than derived from [dev.devscroll.BuildConfig.API_BASE_URL]:
 * the documents are identical in every environment, and a `local` build would otherwise
 * point a tester at an unreachable LAN address.
 */
object ExternalLinks {
    private const val LEGAL_BASE = "https://devscroll.in"

    const val PRIVACY = "$LEGAL_BASE/privacy"
    const val TERMS = "$LEGAL_BASE/terms"
    const val CONTACT = "$LEGAL_BASE/contact"

    /** Must stay identical to the address in the website, both legal docs, and the Play listing. */
    const val SUPPORT_EMAIL = "support@devscroll.in"
}

/** Opens [url] in the user's browser, matching the reader's "open in browser" behaviour. */
fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens a mail composer addressed to [address].
 *
 * Uses ACTION_SENDTO with a mailto: URI rather than ACTION_SEND, so the chooser offers
 * mail clients only instead of every app that accepts text.
 */
fun Context.sendEmail(address: String, subject: String = "", body: String = "") {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:$address".toUri()
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching { startActivity(intent) }.onFailure {
        // The address is always visible on screen, so the user can still copy it.
        Toast.makeText(this, "No email app found — copy the address instead", Toast.LENGTH_LONG).show()
    }
}

/**
 * Reports an article to support, prefilled so the message arrives actionable.
 *
 * This is the app's content-report path for aggregated third-party articles, and it's
 * what the website's contact page points users to.
 */
fun Context.reportArticle(articleId: Long, title: String, url: String, source: String) {
    sendEmail(
        address = ExternalLinks.SUPPORT_EMAIL,
        subject = "Report: $title",
        body = buildString {
            appendLine("Article ID: $articleId")
            appendLine("URL: $url")
            appendLine("Source: $source")
            appendLine()
            appendLine("Please describe the problem:")
            appendLine()
        },
    )
}

/**
 * Shares an article via the system sheet.
 *
 * Sends title and link as one text blob rather than using EXTRA_SUBJECT alone, since
 * most targets (messaging apps, Slack) ignore the subject and would otherwise receive
 * a bare URL with no context.
 */
fun Context.shareArticle(title: String, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n$url")
    }
    runCatching { startActivity(Intent.createChooser(intent, "Share article")) }.onFailure {
        Toast.makeText(this, "Nothing available to share with", Toast.LENGTH_SHORT).show()
    }
}
