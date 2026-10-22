package com.crochetquest.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.GameViewModel
import com.crochetquest.app.ui.GameViewModelFactory
import com.crochetquest.app.ui.screens.home.HomeScreen
import com.crochetquest.app.ui.screens.onboarding.OnboardingScreen
import com.crochetquest.app.ui.screens.placeholder.ComingSoonScreen
import com.crochetquest.app.ui.screens.settings.SettingsScreen

private const val SETTINGS_ROUTE = "settings"

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    QUESTS("quests", "Quests", Icons.Filled.Star),
    PROJECTS("projects", "Projects", Icons.Filled.Create),
    COLLECTION("collection", "Collection", Icons.Filled.Favorite)
}

@Composable
fun CrochetQuestRoot(
    viewModel: GameViewModel = viewModel(factory = GameViewModelFactory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            GameUiState.Loading -> Box(Modifier.fillMaxSize())
            GameUiState.NeedsOnboarding -> OnboardingScreen(onStart = viewModel::completeOnboarding)
            is GameUiState.Ready -> MainScaffold(s, viewModel, snackbarHostState)
        }
    }
}

@Composable
private fun MainScaffold(
    state: GameUiState.Ready,
    viewModel: GameViewModel,
    snackbarHostState: SnackbarHostState
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    state = state,
                    onLogSession = viewModel::logSession,
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) }
                )
            }
            composable(TopLevelDestination.QUESTS.route) {
                ComingSoonScreen(
                    title = "Quest Board",
                    description = "Cozy, creative and project quests with rewards to claim."
                )
            }
            composable(TopLevelDestination.PROJECTS.route) {
                ComingSoonScreen(
                    title = "Project Tracker",
                    description = "Keep track of your real crochet projects, from planned to finished."
                )
            }
            composable(TopLevelDestination.COLLECTION.route) {
                ComingSoonScreen(
                    title = "Collection",
                    description = "Achievements, your wardrobe and the cat's cozy accessories."
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRenameCat = viewModel::renameCat,
                    onAnimationsChange = viewModel::setAnimationsEnabled
                )
            }
        }
    }
}
