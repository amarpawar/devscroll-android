package dev.devscroll.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier,
    screenLabel: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (navigationIcon != null) 4.dp else 16.dp,
                end = 4.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon?.invoke()
        Text(
            text = "> devscroll",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        AnimatedContent(
            targetState = screenLabel,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "screenLabel",
        ) { label ->
            if (label != null) {
                Text(
                    text = "/$label",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "_",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer { alpha = cursorAlpha },
        )
        Spacer(modifier = Modifier.weight(1f))
        actions()
        TerminalIconButton(
            icon = if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
            contentDescription = "Toggle theme",
            onClick = onThemeToggle,
        )
    }
}
