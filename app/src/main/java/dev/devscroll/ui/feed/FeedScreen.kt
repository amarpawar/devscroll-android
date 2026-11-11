package dev.devscroll.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.components.ArticleCard
import dev.devscroll.ui.components.FeedStyle
import dev.devscroll.ui.components.BlinkingEmptyState
import dev.devscroll.ui.components.TerminalLoader
import dev.devscroll.ui.components.TerminalLoadMore
import dev.devscroll.ui.components.TerminalToast
import dev.devscroll.ui.PaginationConfig
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import dev.devscroll.ui.components.TerminalDivider
import dev.devscroll.ui.components.TerminalIconButton
import dev.devscroll.ui.components.TerminalScrollTopButton
import dev.devscroll.ui.components.TopBarActions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onArticleClick: (Long) -> Unit,
    onClearFilter: () -> Unit = {},
    onClearBlogFilter: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    tabClickTrigger: Long = 0L,
    onSearchClick: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val feedStyle by viewModel.feedStyle.collectAsState()
    TopBarActions {
        FeedStyleMenu(selected = feedStyle, onSelect = viewModel::setFeedStyle)
        TerminalIconButton(Icons.Default.Search, "Search", onClick = onSearchClick)
    }

    val uiState by viewModel.uiState.collectAsState()
    val savingArticles by viewModel.savingArticles.collectAsState()
    val user by viewModel.userState.collectAsState()

    var showAuthToast by remember { mutableStateOf(false) }

    LaunchedEffect(showAuthToast) {
        if (showAuthToast) {
            kotlinx.coroutines.delay(2500.milliseconds)
            showAuthToast = false
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isScrolledFromTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    val onArticleClicked = remember(viewModel, onArticleClick) {
        { article: dev.devscroll.data.model.Article ->
            viewModel.markRead(article.id)
            onArticleClick(article.id)
        }
    }
    val onSaveClicked = remember(viewModel, user) {
        { article: dev.devscroll.data.model.Article ->
            if (user == null) {
                showAuthToast = true
            } else {
                viewModel.toggleSave(article.id)
            }
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.isBatchLimitReached) {
        if (shouldLoadMore && !uiState.isBatchLimitReached) {
            viewModel.loadNextPage()
        }
    }


    LaunchedEffect(tabClickTrigger) {
        if (tabClickTrigger > 0L) {
            listState.animateScrollToItem(0)
            viewModel.resetAndRefresh()
        }
    }

    // Reset to the top only when the filters genuinely changed. Keying a LaunchedEffect
    // on them alone also fires on every entry into composition, which discarded the
    // scroll position restored when returning from the reader. Saveable so the
    // comparison survives navigating away and back.
    val filterKey = "${uiState.sortOrder}|${uiState.activeCategoryFilters.sorted()}|" +
            "${uiState.searchQuery}|${uiState.activeBlogFilter}"
    var lastFilterKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(filterKey) {
        if (lastFilterKey != null && lastFilterKey != filterKey) {
            listState.scrollToItem(0)
        }
        lastFilterKey = filterKey
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isSearchActive) {
            // Search banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$ search --query=\"${uiState.searchQuery}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TerminalIconButton(Icons.Default.Close, "Clear search", buttonSize = 32.dp, iconSize = 14.dp) { viewModel.clearSearch() }
            }
            TerminalDivider()
        } else {
            // Active filter banner
            if (uiState.activeCategoryFilters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$ filter --tag=${uiState.activeCategoryFilters.joinToString(", ") { "#$it" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TerminalIconButton(Icons.Default.Close, "Clear filter", buttonSize = 32.dp, iconSize = 14.dp) { onClearFilter() }
                }
                TerminalDivider()
            }

            if (uiState.activeBlogFilter != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$ filter source=@${uiState.activeBlogName ?: uiState.activeBlogFilter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TerminalIconButton(Icons.Default.Close, "Clear source filter", buttonSize = 32.dp, iconSize = 14.dp) { onClearBlogFilter() }
                }
                TerminalDivider()
            }

            // Sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val labelSmall = MaterialTheme.typography.labelSmall
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val primaryColor = MaterialTheme.colorScheme.primary

                val annotatedText = remember(uiState.sortOrder, labelSmall, onSurfaceVariant, primaryColor) {
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = labelSmall.fontSize,
                                fontWeight = labelSmall.fontWeight,
                                fontFamily = labelSmall.fontFamily,
                                letterSpacing = labelSmall.letterSpacing
                            )
                        ) {
                            withStyle(
                                style = SpanStyle(
                                    color = onSurfaceVariant,
                                )
                            ) {
                                append("//sorted by ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = primaryColor
                                )
                            ) {
                                append(if (uiState.sortOrder == SortOrder.NEWEST) "↓ newest" else "↑ trending")
                            }
                        }
                    }
                }
                Text(
                    text = annotatedText,
                    modifier = Modifier
                        .clickable {
                            viewModel.toggleSort()
                        }
                        .padding(4.dp),
                )
                Text(
                    text = "${uiState.articles.size} articles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TerminalDivider()
        } // end else (not search active)

        // Error
        if (uiState.error != null) {
            Text(
                text = "error: ${uiState.error}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        if (uiState.articles.isNotEmpty() && uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                TerminalLoader(
                    labels = listOf("HANDSHAKE", "AUTH OK", "GET /feed"),
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
            }
            TerminalDivider()
        }

        when {
            uiState.articles.isEmpty() && uiState.isRefreshing -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TerminalLoader(
                        labels = if (uiState.isSearchActive)
                            listOf("SEARCH", "GET /search")
                        else
                            listOf("HANDSHAKE", "AUTH OK", "GET /feed"),
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                }
            }
            uiState.articles.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        BlinkingEmptyState("no articles. pull to refresh")
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f),
                    indicator = {},
                ) {
                    LazyColumn(state = listState) {
                        items(
                            items = uiState.articles,
                            key = { it.id },
                            contentType = { "article" }
                        ) { article ->
                            ArticleCard(
                                article = article,
                                onArticleClick = onArticleClicked,
                                onSaveClick = onSaveClicked,
                                isSaving = article.id in savingArticles,
                                style = feedStyle,
                            )
                            TerminalDivider()
                        }
                        if (uiState.isLoadingMore) {
                            item(contentType = "loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TerminalLoader(
                                        labels = listOf("GET /feed"),
                                        modifier = Modifier.fillMaxWidth(0.6f),
                                    )
                                }
                            }
                        }

                        if (uiState.isBatchLimitReached && uiState.hasNext) {
                            item(contentType = "batch_loader") {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    TerminalLoadMore(
                                        itemCount = PaginationConfig.maxItemsPerBatch,
                                        onClick = {
                                            scope.launch {
                                                listState.scrollToItem(0)
                                            }
                                            viewModel.loadNextBatch()
                                        }
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    // Scroll-to-top: hidden at the top of the feed, appears once the list is scrolled
    AnimatedVisibility(
        visible = isScrolledFromTop && uiState.articles.isNotEmpty() && !showAuthToast,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        TerminalScrollTopButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } }
        )
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
            actionLabel = "save articles",
            commandLabel = "save",
            modifier = Modifier.clickable {
                showAuthToast = false
                onSignInClick()
            }
        )
    }
}
}

@Composable
private fun FeedStyleMenu(selected: FeedStyle, onSelect: (FeedStyle) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        TerminalIconButton(selected.icon, "Feed Style") { menuOpen = true }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            FeedStyle.entries.forEachIndexed { index, style ->
                if (index > 0) TerminalDivider()
                FeedStyleItem(style, isSelected = style == selected) {
                    onSelect(style)
                    menuOpen = false
                }
            }
        }
    }
}

@Composable
private fun FeedStyleItem(style: FeedStyle, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    DropdownMenuItem(
        text = { Text(style.label, style = MaterialTheme.typography.bodyMedium, color = color) },
        leadingIcon = {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        // Every item keeps the trailing slot, dot or not. Otherwise the slot only exists on the
        // selected row, and the menu's width changes with which row that is.
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier),
            )
        },
        onClick = onClick,
    )
}
