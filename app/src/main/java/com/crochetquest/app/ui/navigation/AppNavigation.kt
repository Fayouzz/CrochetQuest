package com.crochetquest.app.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.crochetquest.app.domain.projects.categoryEnum
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.GameViewModel
import com.crochetquest.app.ui.GameViewModelFactory
import com.crochetquest.app.ui.LevelUpEvent
import com.crochetquest.app.ui.components.LevelUpCelebration
import com.crochetquest.app.ui.components.PixelSprite
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites
import com.crochetquest.app.ui.feedback.FeedbackCue
import com.crochetquest.app.ui.feedback.rememberFeedback
import com.crochetquest.app.ui.screens.collection.CollectionScreen
import com.crochetquest.app.ui.screens.home.HomeScreen
import com.crochetquest.app.ui.screens.onboarding.OnboardingScreen
import com.crochetquest.app.ui.screens.projects.ProjectDetailScreen
import com.crochetquest.app.ui.screens.projects.ProjectEditorScreen
import com.crochetquest.app.ui.screens.projects.ProjectsTab
import com.crochetquest.app.ui.screens.quests.QuestsScreen
import com.crochetquest.app.ui.screens.room.RoomScreen
import com.crochetquest.app.ui.screens.settings.SettingsScreen
import com.crochetquest.app.ui.screens.stats.StatsScreen
import com.crochetquest.app.ui.screens.wardrobe.WardrobeScreen
import com.crochetquest.app.ui.share.ShareCard
import com.crochetquest.app.ui.share.ShareCardData
import com.crochetquest.app.ui.util.rememberAnimationsAllowed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SETTINGS_ROUTE = "settings"
private const val WARDROBE_ROUTE = "wardrobe"
private const val STATS_ROUTE = "stats"
private const val PROJECT_DETAIL_ROUTE = "project/{id}"
private const val PROJECT_EDIT_ROUTE = "project_edit/{id}"
private const val ID_ARG = "id"

private fun projectDetailRoute(id: Long) = "project/$id"
private fun projectEditRoute(id: Long) = "project_edit/$id" // id 0 means "new project"

/** A bottom-bar tab. It has either a Material icon or a little pixel-art sprite. */
private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val sprite: PixelSprite? = null
) {
    HOME("home", "Home", icon = Icons.Filled.Home),
    QUESTS("quests", "Quests", icon = Icons.Filled.Star),
    PROJECTS("projects", "Projects", icon = Icons.Filled.Create),
    ROOM("room", "Room", sprite = Sprites.plant),
    COLLECTION("collection", "Collection", icon = Icons.Filled.Favorite)
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
            is GameUiState.Ready -> ReadyContent(
                state = s,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                celebration = celebration,
                onDismissCelebration = { celebration = null }
            )
        }
    }
}

/** Everything shown once the player has a profile: the tabs, sounds, reactions and celebration. */
@Composable
private fun ReadyContent(
    state: GameUiState.Ready,
    viewModel: GameViewModel,
    snackbarHostState: SnackbarHostState,
    celebration: LevelUpEvent?,
    onDismissCelebration: () -> Unit
) {
    val feedback = rememberFeedback(state.soundEnabled)
    var reactionTick by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.cues.collect { feedback(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.catReactions.collect { reactionTick += 1 }
    }

    Box(Modifier.fillMaxSize()) {
        MainScaffold(state, viewModel, snackbarHostState, feedback, reactionTick)
        celebration?.let { event ->
            LevelUpCelebration(
                event = event,
                animate = rememberAnimationsAllowed(state.animationsEnabled),
                onDismiss = onDismissCelebration
            )
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
    snackbarHostState: SnackbarHostState,
    feedback: (FeedbackCue) -> Unit,
    reactionTick: Int
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }
    val restorePreview by viewModel.restorePreview.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                            icon = {
                                if (destination.icon != null) {
                                    Icon(destination.icon, contentDescription = null)
                                } else if (destination.sprite != null) {
                                    PixelSpriteImage(destination.sprite, Modifier.size(24.dp))
                                }
                            },
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
                    reactionTick = reactionTick,
                    onFeedback = feedback,
                    onLogSession = viewModel::logSession,
                    onStartTimer = viewModel::startTimer,
                    onCancelTimer = viewModel::cancelTimer,
                    onFinishTimer = viewModel::finishTimer,
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenQuests = { navController.navigateTopLevel(TopLevelDestination.QUESTS.route) },
                    onOpenProjects = { navController.navigateTopLevel(TopLevelDestination.PROJECTS.route) },
                    onAddProject = { navController.navigate(projectEditRoute(0)) },
                    onOpenProject = { navController.navigate(projectDetailRoute(it)) },
                    onClaimQuest = viewModel::claimQuest,
                    onOpenWardrobe = { navController.navigate(WARDROBE_ROUTE) },
                    onOpenStats = { navController.navigate(STATS_ROUTE) }
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
                ProjectsTab(
                    state = state,
                    onAddProject = { navController.navigate(projectEditRoute(0)) },
                    onOpenProject = { navController.navigate(projectDetailRoute(it)) },
                    onSaveStash = viewModel::saveStash,
                    onAdjustStash = viewModel::adjustStash,
                    onDeleteStash = viewModel::deleteStash
                )
            }
            composable(TopLevelDestination.ROOM.route) {
                RoomScreen(
                    state = state,
                    onBuy = viewModel::purchaseRoom,
                    onPlace = viewModel::placeRoom,
                    onRemove = viewModel::removeRoom
                )
            }
            composable(TopLevelDestination.COLLECTION.route) {
                CollectionScreen(
                    achievements = state.achievements,
                    wardrobe = state.wardrobe,
                    catName = state.catName,
                    finishedProjects = state.finishedProjects,
                    onOpenWardrobe = { navController.navigate(WARDROBE_ROUTE) },
                    onOpenProject = { navController.navigate(projectDetailRoute(it)) },
                    onOpenStats = { navController.navigate(STATS_ROUTE) }
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRenameCat = viewModel::renameCat,
                    onAnimationsChange = viewModel::setAnimationsEnabled,
                    onSoundChange = viewModel::setSoundEnabled,
                    onThemeChange = viewModel::setThemeMode,
                    onResetProgress = viewModel::resetProgress,
                    onExport = viewModel::exportBackup,
                    onRestoreFile = viewModel::prepareRestore,
                    restorePreview = restorePreview,
                    onConfirmRestore = viewModel::confirmRestore,
                    onCancelRestore = viewModel::cancelRestore
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
            composable(STATS_ROUTE) {
                StatsScreen(state = state, onBack = { navController.popBackStack() })
            }
            composable(
                route = PROJECT_DETAIL_ROUTE,
                arguments = listOf(navArgument(ID_ARG) { type = NavType.LongType })
            ) { entry ->
                val projectId = entry.arguments?.getLong(ID_ARG) ?: 0L
                val project = state.projects.firstOrNull { it.id == projectId }
                val updatesFlow = remember(projectId) { viewModel.observeProjectUpdates(projectId) }
                val updates by updatesFlow.collectAsState(initial = emptyList<ProjectUpdate>())
                val linkedYarnIds = state.stash
                    .filter { projectId in it.projectIds }
                    .map { it.yarn.id }
                    .toSet()
                ProjectDetailScreen(
                    project = project,
                    updates = updates,
                    stash = state.stash,
                    linkedYarnIds = linkedYarnIds,
                    onSetYarns = { viewModel.setProjectYarns(projectId, it) },
                    onShare = {
                        if (project != null) {
                            scope.launch {
                                val data = ShareCardData(
                                    catName = state.catName,
                                    accessoryKeys = state.equippedKeys,
                                    projectName = project.name,
                                    category = project.categoryEnum,
                                    completedAtMillis = project.completedAt,
                                    night = state.isNight
                                )
                                try {
                                    val file = withContext(Dispatchers.Default) {
                                        ShareCard.createFile(context, data, project.id)
                                    }
                                    ShareCard.share(context, file, project.name)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Couldn't create the share card.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
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
