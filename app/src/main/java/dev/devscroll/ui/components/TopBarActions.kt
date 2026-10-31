package dev.devscroll.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry

/**
 * Lets each screen declare its own top-bar actions while the bar itself is drawn once by the
 * Scaffold (so it stays fixed during tab slides). Actions are keyed by back-stack entry id, so
 * during a transition only the current destination's actions are shown.
 */
@Stable
class TopBarActionsRegistry {
    private val actions = mutableStateMapOf<String, State<@Composable RowScope.() -> Unit>>()

    internal fun register(entryId: String, content: State<@Composable RowScope.() -> Unit>) {
        actions[entryId] = content
    }

    internal fun unregister(entryId: String, content: State<@Composable RowScope.() -> Unit>) {
        // Only remove if it's still ours — avoids a late dispose wiping a fresh registration.
        if (actions[entryId] === content) actions.remove(entryId)
    }

    /**
     * Renders the actions registered by [entryId], if any. Swaps cross-fade (matching the top
     * bar's screen label) so the one-frame gap before a new screen registers isn't a hard pop.
     */
    @Composable
    fun Render(entryId: String?) {
        // Target the State holder, not its lambda: it stays stable while the screen recomposes,
        // so only a real change of screen animates. It also outlives unregister, letting the
        // outgoing actions keep rendering while they fade out.
        val target = entryId?.let { actions[it] }
        AnimatedContent(
            targetState = target,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "topBarActions",
        ) { state ->
            if (state != null) {
                Row(verticalAlignment = Alignment.CenterVertically) { state.value(this) }
            }
        }
    }
}

val LocalTopBarActions = staticCompositionLocalOf { TopBarActionsRegistry() }

/**
 * Call from inside a NavHost destination to contribute actions to the shared top bar.
 * The actions are removed automatically when the destination leaves composition.
 */
@Composable
fun TopBarActions(content: @Composable RowScope.() -> Unit) {
    val registry = LocalTopBarActions.current
    val entryId = (LocalViewModelStoreOwner.current as? NavBackStackEntry)?.id ?: return
    val currentContent = rememberUpdatedState(content)
    DisposableEffect(registry, entryId) {
        registry.register(entryId, currentContent)
        onDispose { registry.unregister(entryId, currentContent) }
    }
}
