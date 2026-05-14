package dev.devscroll.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val themeState = themePreferences.themeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeState())

    fun toggleDark() {
        viewModelScope.launch { themePreferences.setDark(!themeState.value.isDark) }
    }

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { themePreferences.setAccent(accent) }
    }
}
