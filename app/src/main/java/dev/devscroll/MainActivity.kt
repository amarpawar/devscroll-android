package dev.devscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import dev.devscroll.data.local.AuthPreferences
import dev.devscroll.data.model.User
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.ui.feed.FeedScreen
import dev.devscroll.ui.feed.FeedViewModel
import dev.devscroll.ui.contact.ContactScreen
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
    @Inject lateinit var authPreferences: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeState by themePreferences.themeState.collectAsState(initial = ThemeState())
            val userState by authPreferences.user.collectAsState(initial = null)
            DevscrollTheme(
                darkTheme = themeState.isDark,
                accentColor = themeState.accentColor,
            ) {
                val navController = rememberNavController()
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentEntry?.destination?.route

                var feedTabClickTrigger by remember { androidx.compose.runtime.mutableStateOf(0L) }
                var savedTabClickTrigger by remember { androidx.compose.runtime.mutableStateOf(0L) }

                Scaffold(
                    bottomBar = {
                        if (currentRoute in Screen.bottomNavRoutes) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                user = userState,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.FEED) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onActiveTabClick = { route ->
                                    when (route) {
                                        Screen.FEED -> feedTabClickTrigger = System.currentTimeMillis()
                                        Screen.SAVED -> savedTabClickTrigger = System.currentTimeMillis()
                                    }
                                }
                            )
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.FEED,
                        modifier = Modifier
                            .padding(innerPadding)
                            .pointerInput(currentRoute) {
                                if (currentRoute in Screen.bottomNavRoutes) {
                                    var totalDrag = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { totalDrag = 0f },
                                        onDragEnd = {
                                            val swipeThreshold = 80f
                                            if (totalDrag < -swipeThreshold) {
                                                navigateToAdjacentTab(navController, currentRoute, next = true)
                                            } else if (totalDrag > swipeThreshold) {
                                                navigateToAdjacentTab(navController, currentRoute, next = false)
                                            }
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDrag += dragAmount
                                        }
                                    )
                                }
                            },
                        enterTransition = {
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route
                            val tabRoutes = listOf(Screen.FEED, Screen.SAVED, Screen.SOURCES, Screen.PROFILE)
                            val initialIndex = tabRoutes.indexOf(initialRoute)
                            val targetIndex = tabRoutes.indexOf(targetRoute)
                            
                            if (initialIndex != -1 && targetIndex != -1) {
                                if (targetIndex > initialIndex) {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(300)
                                    )
                                } else {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(300)
                                    )
                                }
                            } else {
                                fadeIn(animationSpec = tween(300))
                            }
                        },
                        exitTransition = {
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route
                            val tabRoutes = listOf(Screen.FEED, Screen.SAVED, Screen.SOURCES, Screen.PROFILE)
                            val initialIndex = tabRoutes.indexOf(initialRoute)
                            val targetIndex = tabRoutes.indexOf(targetRoute)
                            
                            if (initialIndex != -1 && targetIndex != -1) {
                                if (targetIndex > initialIndex) {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(300)
                                    )
                                } else {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(300)
                                    )
                                }
                            } else {
                                fadeOut(animationSpec = tween(300))
                            }
                        },
                        popEnterTransition = {
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route
                            val tabRoutes = listOf(Screen.FEED, Screen.SAVED, Screen.SOURCES, Screen.PROFILE)
                            val initialIndex = tabRoutes.indexOf(initialRoute)
                            val targetIndex = tabRoutes.indexOf(targetRoute)
                            
                            if (initialIndex != -1 && targetIndex != -1) {
                                if (targetIndex > initialIndex) {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(300)
                                    )
                                } else {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(300)
                                    )
                                }
                            } else {
                                fadeIn(animationSpec = tween(300))
                            }
                        },
                        popExitTransition = {
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route
                            val tabRoutes = listOf(Screen.FEED, Screen.SAVED, Screen.SOURCES, Screen.PROFILE)
                            val initialIndex = tabRoutes.indexOf(initialRoute)
                            val targetIndex = tabRoutes.indexOf(targetRoute)
                            
                            if (initialIndex != -1 && targetIndex != -1) {
                                if (targetIndex > initialIndex) {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(300)
                                    )
                                } else {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(300)
                                    )
                                }
                            } else {
                                fadeOut(animationSpec = tween(300))
                            }
                        }
                    ) {
                        composable(Screen.FEED) { backStackEntry ->
                            val feedViewModel: FeedViewModel = hiltViewModel()
                            val filterTagsList by backStackEntry.savedStateHandle
                                .getStateFlow<ArrayList<String>>("filter_tags", arrayListOf())
                                .collectAsState()
                            val filterTags = filterTagsList.toSet()
                            LaunchedEffect(filterTags) {
                                if (feedViewModel.uiState.value.activeCategoryFilters != filterTags) {
                                    feedViewModel.setFilter(filterTags)
                                }
                            }
                            val filterBlogId by backStackEntry.savedStateHandle
                                .getStateFlow<Long?>("filter_blog_id", null)
                                .collectAsState()
                            LaunchedEffect(filterBlogId) {
                                if (feedViewModel.uiState.value.activeBlogFilter != filterBlogId) {
                                    feedViewModel.setBlogFilter(filterBlogId)
                                }
                            }
                            FeedScreen(
                                viewModel = feedViewModel,
                                onArticleClick = { id -> navController.navigate(Screen.reader(id)) },
                                onSearchClick = { navController.navigate(Screen.SEARCH) },
                                onClearFilter = {
                                    feedViewModel.clearFilter()
                                    backStackEntry.savedStateHandle["filter_tags"] = arrayListOf<String>()
                                },
                                onClearBlogFilter = {
                                    feedViewModel.clearBlogFilter()
                                    backStackEntry.savedStateHandle["filter_blog_id"] = null
                                },
                                onSignInClick = {
                                    navController.navigate(Screen.PROFILE) {
                                        popUpTo(Screen.FEED) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                tabClickTrigger = feedTabClickTrigger,
                            )
                        }
                        composable(Screen.SAVED) {
                            SavedScreen(
                                onArticleClick = { id -> navController.navigate(Screen.reader(id)) },
                                onSignInClick = {
                                    navController.navigate(Screen.PROFILE) {
                                        popUpTo(Screen.FEED) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                tabClickTrigger = savedTabClickTrigger,
                            )
                        }
                        composable(Screen.SOURCES) {
                            SourcesScreen(
                                onSearchClick = { navController.navigate(Screen.SEARCH) },
                                onSignInClick = {
                                    navController.navigate(Screen.PROFILE) {
                                        popUpTo(Screen.FEED) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                        composable(Screen.PROFILE) {
                            ProfileScreen(
                                onContactClick = { navController.navigate(Screen.CONTACT) },
                            )
                        }
                        composable(Screen.CONTACT) {
                            ContactScreen()
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
                                onSourceClick = { blogId ->
                                    feedEntry.savedStateHandle["filter_blog_id"] = blogId
                                    navController.popBackStack(Screen.FEED, inclusive = false)
                                },
                                onSearch = { query -> feedViewModel.search(query) },
                                initialFilterTags = currentTags.toSet(),
                                onSignInClick = {
                                    navController.navigate(Screen.PROFILE) {
                                        popUpTo(Screen.FEED) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
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
                                onSignInClick = {
                                    navController.navigate(Screen.PROFILE) {
                                        popUpTo(Screen.FEED) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun navigateToAdjacentTab(
        navController: androidx.navigation.NavHostController,
        currentRoute: String?,
        next: Boolean
    ) {
        val tabRoutes = listOf(
            Screen.FEED,
            Screen.SAVED,
            Screen.SOURCES,
            Screen.PROFILE
        )
        val currentIndex = tabRoutes.indexOf(currentRoute)
        if (currentIndex == -1) return

        val newIndex = if (next) currentIndex + 1 else currentIndex - 1
        if (newIndex in tabRoutes.indices) {
            val targetRoute = tabRoutes[newIndex]
            navController.navigate(targetRoute) {
                popUpTo(Screen.FEED) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
