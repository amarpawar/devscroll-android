package dev.devscroll.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.components.ArticleCard
import dev.devscroll.ui.components.SourceBadge
import dev.devscroll.ui.components.TagChip
import dev.devscroll.ui.components.TerminalLoader
import dev.devscroll.ui.components.TerminalToast
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onArticleClick: (Long) -> Unit,
    onBack: () -> Unit,
    onApplyTags: (List<String>) -> Unit,
    onSourceClick: (Long) -> Unit,
    onSearch: (String) -> Unit = {},
    initialFilterTags: Set<String> = emptySet(),
    onSignInClick: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val topTags by viewModel.topTags.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val isLoadingSources by viewModel.isLoadingSources.collectAsState()
    val user by viewModel.userState.collectAsState()
    val savingArticles by viewModel.savingArticles.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var searchSubmitted by remember { mutableStateOf(false) }

    var showAuthToast by remember { mutableStateOf(false) }

    LaunchedEffect(showAuthToast) {
        if (showAuthToast) {
            kotlinx.coroutines.delay(2500.milliseconds)
            showAuthToast = false
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
    val isIdle = query.isBlank()

    // Local tag selection — initialized from current feed filter
    var selectedTags by rememberSaveable { mutableStateOf(initialFilterTags) }

    val handleBack = {
        when {
            query.isNotBlank() -> viewModel.updateQuery("")
            else -> {
                onApplyTags(selectedTags.toList())
                onBack()
            }
        }
    }

    BackHandler { handleBack() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { handleBack() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
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
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank() && !searchSubmitted) {
                            searchSubmitted = true
                            onSearch(query)
                            onBack()
                        }
                    },
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "grep articles…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                },
            )
            if (query.isNotBlank()) {
                IconButton(onClick = { viewModel.updateQuery("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (isIdle) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "// browse by tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )

                if (topTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TerminalLoader(labels = listOf("loading tags..."))
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        topTags.forEach { tag ->
                            TagChip(
                                label = tag,
                                selected = tag in selectedTags,
                                onClick = {
                                    selectedTags = if (tag in selectedTags)
                                        selectedTags - tag
                                    else
                                        selectedTags + tag
                                },
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                if (isLoadingSources || sources.isNotEmpty()) {
                    Text(
                        text = "// browse by source",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }

                if (isLoadingSources) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TerminalLoader(labels = listOf("fetching sources..."))
                    }
                } else if (sources.isNotEmpty()) {
                    sources.forEach { source ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSourceClick(source.blogId) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SourceBadge(blogName = source.blogName, size = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "@${source.blogName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = "${source.articleCount} articles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            if (results.isNotEmpty()) {
                Text(
                    text = "// ${results.size} match${if (results.size == 1) "" else "es"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            LazyColumn {
                items(
                    items = results,
                    key = { it.id },
                    contentType = { "article" }
                ) { article ->
                    ArticleCard(
                        article = article,
                        onArticleClick = onArticleClicked,
                        onSaveClick = onSaveClicked,
                        isSaving = article.id in savingArticles,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
            modifier = Modifier.clickable {
                showAuthToast = false
                onSignInClick()
            }
        )
    }
}
}
