package dev.devscroll.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import dev.devscroll.data.model.User
import dev.devscroll.navigation.Screen

private data class NavTab(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

private val tabs = listOf(
    NavTab(Screen.FEED, "feed", Icons.Default.DynamicFeed, Icons.Outlined.DynamicFeed),
    NavTab(Screen.SAVED, "saved", Icons.Default.Bookmarks, Icons.Outlined.Bookmarks),
    NavTab(Screen.SOURCES, "sources", Icons.Default.Source, Icons.Outlined.Source),
    NavTab(Screen.PROFILE, "profile", Icons.Default.Person, Icons.Outlined.Person)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    user: User?,
    onNavigate: (String) -> Unit,
    onActiveTabClick: (String) -> Unit = {},
) {
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Measured (left, width) of each tab in root coordinates. Using real bounds
    // instead of width / tabs.size accounts for RTL ordering, the spacing
    // NavigationBar puts between items, and horizontal window insets.
    val tabBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    var containerLeft by remember { mutableFloatStateOf(0f) }
    val indicatorLeft = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }

    // When no tab is selected (selectedIndex == -1) the target is null and the
    // indicator stays where it was, so returning to a tab slides from the last
    // selected tab rather than from the first one.
    val target = tabBounds[selectedIndex]
    LaunchedEffect(target) {
        val (left, width) = target ?: return@LaunchedEffect
        if (indicatorWidth.value == 0f) {
            // First placement: appear in place instead of sliding in from x = 0.
            indicatorLeft.snapTo(left)
            indicatorWidth.snapTo(width)
        } else {
            val spec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
            launch { indicatorLeft.animateTo(left, spec) }
            indicatorWidth.animateTo(width, spec)
        }
    }

    Box(
        modifier = Modifier.onGloballyPositioned { containerLeft = it.positionInRoot().x },
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isProfileTab = tab.route == Screen.PROFILE
                val displayLabel = if (isProfileTab && user != null) {
                    // Generate handle without spaces and lowercase, like @johndoe
                    "@${user.name.lowercase().replace(" ", "")}"
                } else {
                    tab.label
                }

                val isSelected = currentRoute == tab.route
                val icon = if (isSelected) tab.filledIcon else tab.outlinedIcon

                NavigationBarItem(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val bounds = coords.positionInRoot().x to coords.size.width.toFloat()
                        if (tabBounds[index] != bounds) tabBounds[index] = bounds
                    },
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onActiveTabClick(tab.route)
                        } else {
                            onNavigate(tab.route)
                        }
                    },
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
                                    Icon(icon, contentDescription = tab.label)
                                },
                                error = {
                                    Icon(icon, contentDescription = tab.label)
                                }
                            )
                        } else {
                            Icon(icon, contentDescription = tab.label)
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
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        TerminalDivider(modifier = Modifier.align(Alignment.TopCenter))
        if (selectedIndex >= 0) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .drawBehind {
                        val width = indicatorWidth.value
                        if (width > 0f) {
                            drawRect(
                                color = primaryColor,
                                topLeft = Offset(x = indicatorLeft.value - containerLeft, y = 0f),
                                size = Size(width = width, height = size.height),
                            )
                        }
                    },
            )
        }
    }
}
