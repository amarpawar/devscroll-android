package dev.devscroll.ui.feed

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.components.ArticleCard
import dev.devscroll.ui.components.BlinkingEmptyState
import dev.devscroll.ui.components.TopBar
import dev.devscroll.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onArticleClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onClearFilter: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()
    val newArticleCount by viewModel.newArticleCount.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadNextPage()
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) viewModel.clearNewArticleCount()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            isDark = themeState.isDark,
            onThemeToggle = { themeViewModel.toggleDark() },
            onSearchClick = onSearchClick,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

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
                IconButton(onClick = { onClearFilter() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        // Sort row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val annotatedText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
                    )
                ) {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    ) {
                        append("//sorted by ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        append(if (uiState.sortOrder == SortOrder.NEWEST) "↓ newest" else "↑ hot")
                    }
                }
            }
            Text(
                text = annotatedText,
                modifier = Modifier
                    .clickable { viewModel.toggleSort() }
                    .padding(4.dp),
            )
            Text(
                text = "${uiState.articles.size} articles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Error
        if (uiState.error != null) {
            Text(
                text = "error: ${uiState.error}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        when {
            uiState.articles.isEmpty() && uiState.isRefreshing -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            uiState.articles.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingEmptyState("no articles. pull to refresh")
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(state = listState) {
                        items(uiState.articles, key = { it.id }) { article ->
                            ArticleCard(
                                article = article,
                                onArticleClick = {
                                    viewModel.markRead(article.id)
                                    onArticleClick(article.id)
                                },
                                onSaveClick = { viewModel.toggleSave(article.id) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }

                    // New articles banner
                    if (newArticleCount > 0 && !isAtTop) {
                        val shape = RoundedCornerShape(2.dp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                        ) {
                            Text(
                                text = "↑ $newArticleCount new",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(shape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, shape)
                                    .clickable {
                                        viewModel.clearNewArticleCount()
                                        scope.launch { listState.animateScrollToItem(0) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
