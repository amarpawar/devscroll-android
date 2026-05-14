package dev.devscroll.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun TagChip(
    label: String,
    modifier: Modifier = Modifier,
    prefix: String = "#",
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val borderColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline
    val textColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val shape = RoundedCornerShape(2.dp)
    val baseModifier = modifier
        .clip(shape)
        .border(1.dp, borderColor, shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 7.dp, vertical = 3.dp)

    Text(
        text = "$prefix$label",
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = baseModifier,
    )
}
