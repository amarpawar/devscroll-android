package dev.devscroll

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.devscroll.data.local.DatabaseInitializer
import dev.devscroll.util.CrashReporter
import javax.inject.Inject

@HiltAndroidApp
class DevscrollApplication : Application() {

    @Inject lateinit var databaseInitializer: DatabaseInitializer
    @Inject lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        crashReporter.initialise()
        // Caching is session-only: wipe the Room cache once per process start.
        databaseInitializer.start()
    }
}
