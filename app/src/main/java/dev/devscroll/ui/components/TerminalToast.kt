package dev.devscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue

@Composable
fun TerminalToast(
    actionLabel: String,
    commandLabel: String,
    modifier: Modifier = Modifier
) {
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(6.dp),
                clip = false
            )
            .background(
                color = Color(0xFF0F0F14), // Dark terminal bg
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), // Glowing purple border
                shape = RoundedCornerShape(6.dp)
            )
            .clip(RoundedCornerShape(6.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: dots and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // macOS/Terminal colored dots
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFF7768E), CircleShape))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFE0AF68), CircleShape))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFBB9AF7), CircleShape))
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = "auth — required",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color(0xFF565F89), // muted gray
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Line 1: ! auth required to ...
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFE0AF68), // Orange/yellow
                )
                Text(
                    text = "auth required to $actionLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA9B1D6), // light gray
                )
            }
            
            // Line 2: $ devscroll login --to=... █
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFBB9AF7), // Prompt purple
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "devscroll login --to=$commandLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(width = 8.dp, height = 14.dp)
                            .background(Color.White)
                            .graphicsLayer { alpha = cursorAlpha }
                    )
                }
            }
        }
    }
}
