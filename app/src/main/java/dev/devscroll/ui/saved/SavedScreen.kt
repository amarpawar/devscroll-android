package dev.devscroll.ui.saved

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.components.ArticleCard
import dev.devscroll.ui.components.BlinkingEmptyState
import dev.devscroll.ui.components.TerminalLoader
import dev.devscroll.ui.components.TerminalLoadMore
import dev.devscroll.ui.components.TerminalToast
import dev.devscroll.ui.components.TopBar
import dev.devscroll.ui.theme.ThemeViewModel
import dev.devscroll.ui.PaginationConfig
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onArticleClick: (Long) -> Unit,
    onSignInClick: () -> Unit = {},
    tabClickTrigger: Long = 0L,
    viewModel: SavedViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val articles = uiState.articles
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

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
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

    val onArticleClicked = remember(onArticleClick) {
        { article: dev.devscroll.data.model.Article ->
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                isDark = themeState.isDark,
                onThemeToggle = { themeViewModel.toggleDark() },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.weight(1f)
            ) {
                if (articles.isEmpty() && !uiState.isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BlinkingEmptyState("nothing saved yet")
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = articles,
                            key = { it.id },
                            contentType = { "article" }
                        ) { article ->
                            ArticleCard(
                                article = article,
                                onArticleClick = onArticleClicked,
                                onSaveClick = onSaveClicked,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }

                        if (uiState.isLoadingMore) {
                            item(key = "loader", contentType = "loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TerminalLoader(
                                        labels = listOf("CONNECT", "GET /saved-articles", "RENDER"),
                                        modifier = Modifier.fillMaxWidth(0.6f),
                                    )
                                }
                            }
                        }

                        if (uiState.isBatchLimitReached && uiState.hasNext) {
                            item(key = "batch_loader", contentType = "batch_loader") {
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
                cursorAlpha = cursorAlpha,
                modifier = Modifier.clickable {
                    showAuthToast = false
                    onSignInClick()
                }
            )
        }
    }
}
