package dev.devscroll.ui.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import dev.devscroll.ui.components.BlinkingEmptyState
import dev.devscroll.ui.components.SourceBadge
import dev.devscroll.ui.components.TerminalLoader
import dev.devscroll.ui.components.TerminalToast
import dev.devscroll.ui.theme.ThemeViewModel
import kotlin.time.Duration.Companion.milliseconds

fun Modifier.dashedBorder(color: Color, width: Dp = 1.dp, cornerRadius: Dp = 4.dp) = drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onSearchClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    viewModel: SourcesViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()
    val user by viewModel.userState.collectAsState()

    var showAuthToast by remember { mutableStateOf(false) }

    LaunchedEffect(showAuthToast) {
        if (showAuthToast) {
            kotlinx.coroutines.delay(2500.milliseconds)
            showAuthToast = false
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Terminal Header matching the screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "> sources",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(width = 8.dp, height = 16.dp)
                        .background(MaterialTheme.colorScheme.primary)
                        .graphicsLayer { alpha = cursorAlpha }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleSort() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onSearchClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { themeViewModel.toggleDark() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (themeState.isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // User Status / Auth Panel (dashed border placeholder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (user == null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .dashedBorder(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                cornerRadius = 2.dp
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "not signed in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "// tap to sign in & sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "[ sign in ]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onSignInClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "@${user?.name?.lowercase()?.replace(" ", "") ?: "user"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "// signed in via ${user?.provider ?: "email"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "[ profile ]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onSignInClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Subscription Status Bar (e.g. // 12 of 12 subscribed | auto-poll 15m)
            val totalCount = sources.size
            val subscribedCount = sources.count { it.isSubscribed }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "// $subscribedCount of $totalCount subscribed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = "RSS auto-poll",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "auto-poll 15m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            val isRefreshing by viewModel.isRefreshing.collectAsState()

            if (sources.isNotEmpty() && isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TerminalLoader(
                        labels = listOf("CONNECT", "GET /sources"),
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            when {
                sources.isEmpty() && isRefreshing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        TerminalLoader(
                            labels = listOf("CONNECT", "GET /sources"),
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                    }
                }
                sources.isEmpty() -> {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.refreshSources() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BlinkingEmptyState("no sources. pull to refresh")
                        }
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshSources() },
                        modifier = Modifier.weight(1f),
                        indicator = {},
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sources, key = { it.blogId }) { source ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SourceBadge(blogName = source.blogName, size = 36.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        val handle = "@" + source.blogName.lowercase()
                                            .replace(" ", "")
                                            .replace(".", "")
                                        Text(
                                            text = handle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        val domain = source.url
                                            .replace("https://", "")
                                            .replace("http://", "")
                                            .replace("www.", "")
                                            .removeSuffix("/")
                                        val postsText = if (source.articleCount == 1) "1 post" else "${source.articleCount} posts"
                                        Text(
                                            text = "$domain · $postsText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    val isSubscribed = source.isSubscribed
                                    Box(
                                        modifier = Modifier
                                            .clickable {
                                                if (user != null) {
                                                    viewModel.toggleSubscribe(source.blogId)
                                                } else {
                                                    showAuthToast = true
                                                }
                                            }
                                            .background(
                                                color = if (isSubscribed) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSubscribed) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isSubscribed) "[x] subbed" else "[ ] sub",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = if (isSubscribed) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        // Dimming Scrim behind the toast
        if (showAuthToast) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        showAuthToast = false
                    }
            )
        }

        // Terminal Toast overlay matching screenshot design
        AnimatedVisibility(
            visible = showAuthToast,
            enter = fadeIn(animationSpec = tween(250)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            TerminalToast(
                actionLabel = "manage subscriptions",
                commandLabel = "subscribe",
                cursorAlpha = cursorAlpha,
                modifier = Modifier.clickable {
                    showAuthToast = false
                    onSignInClick()
                }
            )
        }
    }
}
