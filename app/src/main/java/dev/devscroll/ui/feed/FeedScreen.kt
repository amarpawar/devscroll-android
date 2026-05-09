package dev.devscroll.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FeedScreen(
    onArticleClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("feed_", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}
