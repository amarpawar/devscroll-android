package dev.devscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dev.devscroll.navigation.Screen
import dev.devscroll.ui.components.BottomNavBar
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.feed.FeedScreen
import dev.devscroll.ui.feed.FeedViewModel
import dev.devscroll.ui.profile.ProfileScreen
import dev.devscroll.ui.reader.ReaderScreen
import dev.devscroll.ui.saved.SavedScreen
import dev.devscroll.ui.search.SearchScreen
import dev.devscroll.ui.sources.SourcesScreen
import dev.devscroll.ui.theme.DevscrollTheme
import dev.devscroll.ui.theme.ThemePreferences
import dev.devscroll.ui.theme.ThemeState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by themePreferences.themeState.collectAsState(initial = ThemeState())
            DevscrollTheme(
                darkTheme = themeState.isDark,
                accentColor = themeState.accentColor,
            ) {
                val navController = rememberNavController()
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute in Screen.bottomNavRoutes) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.FEED) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.FEED,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Screen.FEED) { backStackEntry ->
                            val feedViewModel: FeedViewModel = hiltViewModel()
                            val filterTagsList by backStackEntry.savedStateHandle
                                .getStateFlow<ArrayList<String>>("filter_tags", arrayListOf())
                                .collectAsState()
                            LaunchedEffect(filterTagsList) {
                                feedViewModel.setFilter(filterTagsList.toSet())
                            }
                            FeedScreen(
                                viewModel = feedViewModel,
                                onArticleClick = { id -> navController.navigate(Screen.reader(id)) },
                                onSearchClick = { navController.navigate(Screen.SEARCH) },
                                onClearFilter = {
                                    feedViewModel.clearFilter()
                                    backStackEntry.savedStateHandle["filter_tags"] = arrayListOf<String>()
                                },
                            )
                        }
                        composable(Screen.SAVED) {
                            SavedScreen(
                                onArticleClick = { id -> navController.navigate(Screen.reader(id)) },
                            )
                        }
                        composable(Screen.SOURCES) {
                            SourcesScreen()
                        }
                        composable(Screen.PROFILE) {
                            ProfileScreen()
                        }
                        composable(Screen.SEARCH) {
                            val feedEntry = remember(navController.currentBackStackEntry) { navController.getBackStackEntry(Screen.FEED) }
                            val feedViewModel: FeedViewModel = hiltViewModel(feedEntry)
                            val currentTags by feedEntry
                                .savedStateHandle
                                .getStateFlow<ArrayList<String>>("filter_tags", arrayListOf())
                                .collectAsState()
                            SearchScreen(
                                onArticleClick = { id -> navController.navigate(Screen.reader(id)) },
                                onBack = { navController.popBackStack() },
                                onApplyTags = { tags ->
                                    feedEntry.savedStateHandle["filter_tags"] = ArrayList(tags)
                                },
                                onSearch = { query -> feedViewModel.search(query) },
                                initialFilterTags = currentTags.toSet(),
                            )
                        }
                        composable(
                            route = Screen.READER,
                            arguments = listOf(
                                navArgument(Screen.READER_ARG) { type = NavType.LongType }
                            ),
                        ) { backStackEntry ->
                            val articleId = backStackEntry.arguments?.getLong(Screen.READER_ARG) ?: return@composable
                            ReaderScreen(
                                articleId = articleId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
