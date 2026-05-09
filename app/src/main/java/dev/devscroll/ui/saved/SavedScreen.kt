package dev.devscroll.ui.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SavedScreen(
    onArticleClick: (Long) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("saved_", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}
