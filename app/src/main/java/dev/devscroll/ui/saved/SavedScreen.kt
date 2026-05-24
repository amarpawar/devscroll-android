package dev.devscroll.ui.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.components.ArticleCard
import dev.devscroll.ui.components.BlinkingEmptyState
import dev.devscroll.ui.components.TopBar
import dev.devscroll.ui.theme.ThemeViewModel

import androidx.compose.runtime.remember

@Composable
fun SavedScreen(
    onArticleClick: (Long) -> Unit,
    viewModel: SavedViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val articles by viewModel.articles.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()

    val onArticleClicked = remember(onArticleClick) {
        { article: dev.devscroll.data.model.Article ->
            onArticleClick(article.id)
        }
    }
    val onSaveClicked = remember(viewModel) {
        { article: dev.devscroll.data.model.Article ->
            viewModel.toggleSave(article.id)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            isDark = themeState.isDark,
            onThemeToggle = { themeViewModel.toggleDark() },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (articles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BlinkingEmptyState("nothing saved yet")
            }
        } else {
            LazyColumn {
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
            }
        }
    }
}
