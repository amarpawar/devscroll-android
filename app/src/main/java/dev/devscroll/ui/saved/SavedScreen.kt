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

@Composable
fun SavedScreen(
    onArticleClick: (Long) -> Unit,
    viewModel: SavedViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val articles by viewModel.articles.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()

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
                items(articles, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        onArticleClick = { onArticleClick(article.id) },
                        onSaveClick = { viewModel.toggleSave(article.id) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
