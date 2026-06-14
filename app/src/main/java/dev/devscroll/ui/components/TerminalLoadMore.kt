package dev.devscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TerminalLoadMore(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF0F0F14), // Dark terminal bg
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF23232E), // Subtle terminal outline
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
    ) {
        // Top section: Command and buffer status
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFBB9AF7), // Prompt purple
                )
                Text(
                    text = "devscroll fetch --more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "# $itemCount articles in buffer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF565F89), // muted gray
                )
            }
        }

        // Horizontal Divider line separating top and bottom
        HorizontalDivider(color = Color(0xFF23232E), thickness = 1.dp)

        // Bottom section: Interactive load trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBB9AF7), // Prompt purple
                )
                Text(
                    text = "load more",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFBB9AF7), // Prompt purple
                )
            }

            // "↵ enter" badge
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = Color(0xFF2E2E3A),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "↵ enter",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF565F89), // muted gray
                )
            }
        }
    }
}
