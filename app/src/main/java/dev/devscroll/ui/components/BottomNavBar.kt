package dev.devscroll.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import dev.devscroll.data.model.User
import dev.devscroll.navigation.Screen

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    NavTab(Screen.FEED, "feed", Icons.Default.DynamicFeed),
    NavTab(Screen.SAVED, "saved", Icons.Default.Bookmarks),
    NavTab(Screen.SOURCES, "sources", Icons.Default.Source),
    NavTab(Screen.PROFILE, "profile", Icons.Default.Person)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    user: User?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
    ) {
        tabs.forEach { tab ->
            val isProfileTab = tab.route == Screen.PROFILE
            val displayLabel = if (isProfileTab && user != null) {
                // Generate handle without spaces and lowercase, like @johndoe
                "@${user.name.lowercase().replace(" ", "")}"
            } else {
                tab.label
            }

            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = {
                    if (isProfileTab && user != null && !user.pictureUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = user.pictureUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Icon(tab.icon, contentDescription = tab.label)
                            },
                            error = {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        )
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
