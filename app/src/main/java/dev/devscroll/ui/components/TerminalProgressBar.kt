package dev.devscroll.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val WAVE_DURATION_MS = 900

@Composable
fun TerminalLoader(
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.labelSmall
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    var labelIndex by remember { mutableIntStateOf(0) }
    var hexValue by remember { mutableStateOf(randomHex()) }

    LaunchedEffect("label") {
        while (true) {
            delay(WAVE_DURATION_MS.toLong().milliseconds)
            labelIndex = (labelIndex + 1) % labels.size
        }
    }

    LaunchedEffect("hex") {
        while (true) {
            delay(80L.milliseconds)
            hexValue = randomHex()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "terminal_wave")
    val wavePos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WAVE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePos",
    )

    val textMeasurer = rememberTextMeasurer()
    val charWidthPx = remember(style) {
        textMeasurer.measure("█", style).size.width.coerceAtLeast(1)
    }
    // Reserve the widest label's width up front. Without this the label wraps to a
    // second line when a long one comes up, and the bar resizes on every 900ms swap.
    val density = LocalDensity.current
    val labelWidth = remember(labels, style, density) {
        with(density) {
            labels.maxOf { textMeasurer.measure(it, style).size.width }.toDp()
        }
    }
    var numChars by remember { mutableIntStateOf(20) }
    val headPos = wavePos * (numChars + 4).toFloat()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "▶", style = style, color = primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildString {
                for (i in 0 until numChars) {
                    val dist = headPos - i
                    append(
                        when {
                            dist < 0 -> '·'
                            dist < 1 -> '█'
                            dist < 2 -> '▓'
                            dist < 3 -> '▒'
                            dist < 4 -> '░'
                            else -> '·'
                        }
                    )
                }
            },
            style = style,
            color = primary,
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { size ->
                    numChars = (size.width / charWidthPx).coerceAtLeast(8)
                },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = labels[labelIndex],
            style = style,
            color = onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(labelWidth),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = hexValue,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "■", style = style, color = primary)
    }
}

private fun randomHex(): String {
    val value = (0..0xFFFFFF).random()
    return "0x${value.toString(16).uppercase().padStart(6, '0')}"
}
