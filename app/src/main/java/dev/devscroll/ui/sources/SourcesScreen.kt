package dev.devscroll.ui.sources

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.time.Duration.Companion.milliseconds
import dev.devscroll.ui.components.TerminalDivider
import dev.devscroll.ui.components.TerminalIconButton
import dev.devscroll.ui.components.BracketButton
import dev.devscroll.ui.components.TopBarActions
import androidx.compose.material.icons.filled.Search

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
    onSignInClick: () -> Unit = {},
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    TopBarActions {
        TerminalIconButton(Icons.Default.Search, "Search sources") { viewModel.toggleSearch() }
    }

    val sources by viewModel.sources.collectAsState()
    val visibleSources by viewModel.visibleSources.collectAsState()
    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val query by viewModel.query.collectAsState()
    val user by viewModel.userState.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchOpen) { viewModel.closeSearch() }
    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) focusRequester.requestFocus()
    }

    var showAuthToast by remember { mutableStateOf(false) }

    LaunchedEffect(showAuthToast) {
        if (showAuthToast) {
            kotlinx.coroutines.delay(2500.milliseconds)
            showAuthToast = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isSearchOpen,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { viewModel.updateQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "grep sources…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            },
                        )
                        TerminalIconButton(Icons.Default.Close, "Close search", buttonSize = 36.dp, iconSize = 16.dp) { viewModel.closeSearch() }
                    }
                    TerminalDivider()
                }
            }

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
                    BracketButton("sign in") { onSignInClick() }
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
                    BracketButton("profile") { onSignInClick() }
                }
            }
            TerminalDivider()

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
            TerminalDivider()

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
                TerminalDivider()
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            BlinkingEmptyState("no sources. pull to refresh")
                        }
                    }
                }
                visibleSources.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        BlinkingEmptyState("no sources match \"${query.trim()}\"")
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
                            items(visibleSources, key = { it.blogId }) { source ->
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
                                TerminalDivider()
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
                modifier = Modifier.clickable {
                    showAuthToast = false
                    onSignInClick()
                }
            )
        }
    }
}
