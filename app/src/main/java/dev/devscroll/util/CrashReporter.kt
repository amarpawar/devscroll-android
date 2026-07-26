package dev.devscroll.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.devscroll.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash and non-fatal reporting.
 *
 * Firebase only initialises when `app/google-services.json` is present at build time, so
 * every call is guarded: without it this degrades to logcat rather than throwing. That
 * keeps the app buildable and runnable for anyone who hasn't set up Firebase.
 */
@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val crashlytics: FirebaseCrashlytics? by lazy {
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseCrashlytics.getInstance()
        }.getOrNull()
    }

    /** Call once from Application.onCreate. Debug crashes would only pollute the dashboard. */
    fun initialise() {
        crashlytics?.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
    }

    /** Reports a handled failure — something the user recovered from but we want counted. */
    fun recordNonFatal(throwable: Throwable, context: String) {
        val reporter = crashlytics
        if (reporter == null) {
            Log.w(TAG, "non-fatal [$context]", throwable)
            return
        }
        reporter.log(context)
        reporter.recordException(throwable)
    }

    private companion object {
        const val TAG = "CrashReporter"
    }
}
