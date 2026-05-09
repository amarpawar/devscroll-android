package dev.devscroll.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.devscroll.navigation.Screen

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    NavTab(Screen.FEED, "feed", Icons.Default.DynamicFeed),
    NavTab(Screen.SAVED, "saved", Icons.Default.Bookmarks),
    NavTab(Screen.SOURCES, "sources", Icons.Default.Source),
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
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
