package dev.devscroll.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AccentColor(
    val darkColor: Color,
    val lightColor: Color,
    val darkContainer: Color,
    val lightContainer: Color,
) {
    Green(AccentGreenDark, AccentGreenLight, AccentGreenContainerDark, AccentGreenContainerLight),
    Cyan(AccentCyanDark, AccentCyanLight, AccentCyanContainerDark, AccentCyanContainerLight),
    Amber(AccentAmberDark, AccentAmberLight, AccentAmberContainerDark, AccentAmberContainerLight),
    Pink(AccentPinkDark, AccentPinkLight, AccentPinkContainerDark, AccentPinkContainerLight),
    Purple(AccentPurpleDark, AccentPurpleLight, AccentPurpleContainerDark, AccentPurpleContainerLight),
}

private fun buildColorScheme(isDark: Boolean, accent: AccentColor) = if (isDark) {
    val accentColor = accent.darkColor
    darkColorScheme(
        primary = accentColor,
        onPrimary = DarkBg,
        primaryContainer = accent.darkContainer,
        onPrimaryContainer = accentColor,
        secondary = DarkMuted,
        onSecondary = DarkBg,
        secondaryContainer = DarkSurface,
        onSecondaryContainer = DarkText,
        background = DarkBg,
        onBackground = DarkText,
        surface = DarkSurface,
        onSurface = DarkText,
        surfaceVariant = DarkBorder,
        onSurfaceVariant = DarkMuted,
        outline = DarkBorder,
        outlineVariant = DarkBorder,
        error = AccentPinkDark,
        onError = DarkBg,
        inverseSurface = DarkText,
        inverseOnSurface = DarkBg,
        inversePrimary = accent.lightColor,
        scrim = Color(0xCC0D0D0D),
    )
} else {
    val accentColor = accent.lightColor
    lightColorScheme(
        primary = accentColor,
        onPrimary = LightBg,
        primaryContainer = accent.lightContainer,
        onPrimaryContainer = accentColor,
        secondary = LightMuted,
        onSecondary = LightBg,
        secondaryContainer = LightSurface,
        onSecondaryContainer = LightText,
        background = LightBg,
        onBackground = LightText,
        surface = LightSurface,
        onSurface = LightText,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightMuted,
        outline = LightBorder,
        outlineVariant = LightBorder,
        error = AccentPinkLight,
        onError = LightBg,
        inverseSurface = LightText,
        inverseOnSurface = LightBg,
        inversePrimary = accent.darkColor,
        scrim = Color(0x99000000),
    )
}

@Composable
fun DevscrollTheme(
    darkTheme: Boolean = true,
    accentColor: AccentColor = AccentColor.Green,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = buildColorScheme(darkTheme, accentColor),
        typography = Typography,
        content = content,
    )
}
