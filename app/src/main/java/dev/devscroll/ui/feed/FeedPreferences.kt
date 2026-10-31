package dev.devscroll.ui.feed

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.devscroll.ui.components.FeedStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.feedDataStore by preferencesDataStore(name = "feed_prefs")

@Singleton
class FeedPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val FEED_STYLE = stringPreferencesKey("feed_style")

    /** Stored by enum name; an unknown value (e.g. a renamed style) falls back to the default. */
    val feedStyle: Flow<FeedStyle> = context.feedDataStore.data.map { prefs ->
        prefs[FEED_STYLE]
            ?.let { runCatching { FeedStyle.valueOf(it) }.getOrNull() }
            ?: DEFAULT_FEED_STYLE
    }

    suspend fun setFeedStyle(style: FeedStyle) {
        context.feedDataStore.edit { it[FEED_STYLE] = style.name }
    }

    companion object {
        val DEFAULT_FEED_STYLE = FeedStyle.BIG_THUMBNAILS
    }
}
