package dev.devscroll.data.local

import dev.devscroll.di.ApplicationScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears the Room cache once per process, so caching stays session-only.
 *
 * The wipe used to live in MainActivity.onCreate, which also runs on rotation and
 * theme changes; doing it here ties it to the process instead. Readers call
 * [awaitReady] so they can never observe a half-cleared database.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val database: AppDatabase,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val ready = CompletableDeferred<Unit>()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            try {
                database.clearAllTables()
            } finally {
                ready.complete(Unit)
            }
        }
    }

    suspend fun awaitReady() {
        start()
        ready.await()
    }
}
