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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.GameViewModel
import com.crochetquest.app.ui.GameViewModelFactory
import com.crochetquest.app.ui.LevelUpEvent
import com.crochetquest.app.ui.components.LevelUpCelebration
import com.crochetquest.app.ui.screens.wardrobe.WardrobeScreen
import com.crochetquest.app.ui.util.rememberAnimationsAllowed
import com.crochetquest.app.ui.screens.collection.CollectionScreen
import com.crochetquest.app.ui.screens.home.HomeScreen
import com.crochetquest.app.ui.screens.onboarding.OnboardingScreen
import com.crochetquest.app.ui.screens.projects.ProjectDetailScreen
import com.crochetquest.app.ui.screens.projects.ProjectEditorScreen
import com.crochetquest.app.ui.screens.projects.ProjectsScreen
import com.crochetquest.app.ui.screens.quests.QuestsScreen
import com.crochetquest.app.ui.screens.settings.SettingsScreen

private const val SETTINGS_ROUTE = "settings"
private const val WARDROBE_ROUTE = "wardrobe"
private const val PROJECT_DETAIL_ROUTE = "project/{id}"
private const val PROJECT_EDIT_ROUTE = "project_edit/{id}"
private const val ID_ARG = "id"

private fun projectDetailRoute(id: Long) = "project/$id"
private fun projectEditRoute(id: Long) = "project_edit/$id" // id 0 means "new project"

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

    var celebration by remember { mutableStateOf<LevelUpEvent?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.levelUps.collect { celebration = it }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            GameUiState.Loading -> Box(Modifier.fillMaxSize())
            GameUiState.NeedsOnboarding -> OnboardingScreen(onStart = viewModel::completeOnboarding)
            is GameUiState.Ready -> Box(Modifier.fillMaxSize()) {
                MainScaffold(s, viewModel, snackbarHostState)
                celebration?.let { event ->
                    LevelUpCelebration(
                        event = event,
                        animate = rememberAnimationsAllowed(s.animationsEnabled),
                        onDismiss = { celebration = null }
                    )
                }
            }
        }
    }
}

private fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
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
                            onClick = { navController.navigateTopLevel(destination.route) },
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
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenQuests = { navController.navigateTopLevel(TopLevelDestination.QUESTS.route) },
                    onOpenProjects = { navController.navigateTopLevel(TopLevelDestination.PROJECTS.route) },
                    onAddProject = { navController.navigate(projectEditRoute(0)) },
                    onOpenProject = { navController.navigate(projectDetailRoute(it)) },
                    onClaimQuest = viewModel::claimQuest,
                    onOpenWardrobe = { navController.navigate(WARDROBE_ROUTE) }
                )
            }
            composable(TopLevelDestination.QUESTS.route) {
                QuestsScreen(
                    items = state.questBoard,
                    onClaim = viewModel::claimQuest,
                    onMarkDone = viewModel::markQuestDone
                )
            }
            composable(TopLevelDestination.PROJECTS.route) {
                ProjectsScreen(
                    projects = state.projects,
                    onAddProject = { navController.navigate(projectEditRoute(0)) },
                    onOpenProject = { navController.navigate(projectDetailRoute(it)) }
                )
            }
            composable(TopLevelDestination.COLLECTION.route) {
                CollectionScreen(
                    achievements = state.achievements,
                    wardrobe = state.wardrobe,
                    catName = state.catName,
                    onOpenWardrobe = { navController.navigate(WARDROBE_ROUTE) }
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRenameCat = viewModel::renameCat,
                    onAnimationsChange = viewModel::setAnimationsEnabled,
                    onResetProgress = viewModel::resetProgress
                )
            }
            composable(WARDROBE_ROUTE) {
                WardrobeScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onBuy = viewModel::purchase,
                    onEquip = viewModel::equip,
                    onUnequip = viewModel::unequip,
                    onUnequipAll = viewModel::unequipAll
                )
            }
            composable(
                route = PROJECT_DETAIL_ROUTE,
                arguments = listOf(navArgument(ID_ARG) { type = NavType.LongType })
            ) { entry ->
                val projectId = entry.arguments?.getLong(ID_ARG) ?: 0L
                val project = state.projects.firstOrNull { it.id == projectId }
                val updatesFlow = remember(projectId) { viewModel.observeProjectUpdates(projectId) }
                val updates by updatesFlow.collectAsState(initial = emptyList<ProjectUpdate>())
                ProjectDetailScreen(
                    project = project,
                    updates = updates,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(projectEditRoute(projectId)) },
                    onStart = { viewModel.startProject(projectId) },
                    onSetProgress = { viewModel.setProjectProgress(projectId, it) },
                    onFinish = { viewModel.completeProject(projectId) },
                    onReopen = { viewModel.reopenProject(projectId) },
                    onDelete = { viewModel.deleteProject(projectId) { navController.popBackStack() } }
                )
            }
            composable(
                route = PROJECT_EDIT_ROUTE,
                arguments = listOf(navArgument(ID_ARG) { type = NavType.LongType })
            ) { entry ->
                val projectId = entry.arguments?.getLong(ID_ARG) ?: 0L
                val existing = state.projects.firstOrNull { it.id == projectId }
                if (projectId == 0L || existing != null) {
                    ProjectEditorScreen(
                        initial = existing,
                        onSave = { draft -> viewModel.saveProject(draft) { navController.popBackStack() } },
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    // The project vanished (for example it was just deleted): leave the editor.
                    LaunchedEffect(projectId) { navController.popBackStack() }
                }
            }
        }
    }
}
