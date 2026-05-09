package dev.devscroll.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "theme_prefs")

data class ThemeState(
    val isDark: Boolean = true,
    val accentColor: AccentColor = AccentColor.Green,
)

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val IS_DARK = booleanPreferencesKey("is_dark")
    private val ACCENT = stringPreferencesKey("accent")

    val themeState: Flow<ThemeState> = context.dataStore.data.map { prefs ->
        ThemeState(
            isDark = prefs[IS_DARK] ?: true,
            accentColor = prefs[ACCENT]
                ?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() }
                ?: AccentColor.Green,
        )
    }

    suspend fun setDark(isDark: Boolean) {
        context.dataStore.edit { it[IS_DARK] = isDark }
    }

    suspend fun setAccent(accent: AccentColor) {
        context.dataStore.edit { it[ACCENT] = accent.name }
    }
}
