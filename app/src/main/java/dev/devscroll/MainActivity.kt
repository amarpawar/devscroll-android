package dev.devscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.devscroll.ui.feed.FeedScreen
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
                        composable(Screen.FEED) {
                            FeedScreen(
                                onArticleClick = { id -> navController.navigate(Screen.reader(id)) },
                                onSearchClick = { navController.navigate(Screen.SEARCH) },
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
                        composable(Screen.SEARCH) {
                            SearchScreen(
                                onArticleClick = { id -> navController.navigate(Screen.reader(id)) },
                                onBack = { navController.popBackStack() },
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
