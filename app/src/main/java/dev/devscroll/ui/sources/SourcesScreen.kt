package dev.devscroll.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.components.BlinkingEmptyState
import dev.devscroll.ui.components.SourceBadge
import dev.devscroll.ui.components.TopBar
import dev.devscroll.ui.theme.ThemeViewModel

@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            isDark = themeState.isDark,
            onThemeToggle = { themeViewModel.toggleDark() },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (sources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BlinkingEmptyState("no sources yet")
            }
        } else {
            LazyColumn {
                items(sources, key = { it.blogId }) { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SourceBadge(blogName = source.blogName, size = 36.dp)
                        Text(
                            text = source.blogName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${source.articleCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
