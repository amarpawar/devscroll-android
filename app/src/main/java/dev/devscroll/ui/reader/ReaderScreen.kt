package dev.devscroll.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ReaderScreen(
    articleId: Long,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("reader: $articleId _", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}
