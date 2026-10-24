package com.crochetquest.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.crochetquest.app.CrochetQuestApp
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.data.repository.GameRepository
import com.crochetquest.app.data.repository.SettingsRepository
import com.crochetquest.app.domain.achievements.AchievementItem
import com.crochetquest.app.domain.cosmetics.AccessorySlot
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import com.crochetquest.app.domain.cosmetics.PurchaseResult
import com.crochetquest.app.domain.cosmetics.WardrobeItem
import com.crochetquest.app.domain.game.ChecksResult
import com.crochetquest.app.domain.game.ClaimResult
import com.crochetquest.app.domain.game.GameService
import com.crochetquest.app.domain.game.LevelCurve
import com.crochetquest.app.domain.game.LevelInfo
import com.crochetquest.app.domain.game.ProjectResult
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.domain.game.SessionResult
import com.crochetquest.app.domain.projects.ProjectDraft
import com.crochetquest.app.domain.projects.ProjectStatus
import com.crochetquest.app.domain.projects.statusEnum
import com.crochetquest.app.domain.quests.QuestItem
import com.crochetquest.app.domain.quests.pickFeaturedQuest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GameUiState {
    data object Loading : GameUiState
    data object NeedsOnboarding : GameUiState
    data class Ready(
        val catName: String,
        val levelInfo: LevelInfo,
        val yarnBalance: Int,
        val sessionsLogged: Int,
        val stitchesRecorded: Int,
        val rewardedSessionsToday: Int,
        val maxRewardedSessionsPerDay: Int,
        val animationsEnabled: Boolean,
        val projects: List<Project>,
        val questBoard: List<QuestItem>,
        val achievements: List<AchievementItem>,
        val wardrobe: List<WardrobeItem>
    ) : GameUiState {
        val ongoingProjects: List<Project> get() = projects.filter { it.statusEnum != ProjectStatus.COMPLETED }
        val featuredQuest: QuestItem? get() = pickFeaturedQuest(questBoard)
        /** Keys of the accessories the cat is wearing right now. */
        val equippedKeys: List<String> get() = wardrobe.filter { it.equipped }.map { it.item.key }
    }
}

/** Fired when XP crosses a level threshold; drives the short celebration. */
data class LevelUpEvent(val level: Int, val title: String)

class GameViewModel(
    private val repository: GameRepository,
    private val service: GameService,
    private val settings: SettingsRepository
) : ViewModel() {

    /** Emits whenever any stored game data changes, so derived state is rebuilt. */
    private val dataChanged: Flow<Unit> = combine(
        listOf<Flow<Any?>>(
            repository.sessionCount,
            repository.stitchCount,
            repository.projectUpdateCount,
            repository.questCompletions,
            repository.achievementUnlocks,
            repository.inventory,
            repository.equipped
        )
    ) { }

    val uiState: StateFlow<GameUiState> = combine<PlayerProfile?, List<Project>, Boolean, Unit, GameUiState>(
        repository.profile,
        repository.projects,
        settings.animationsEnabled,
        dataChanged
    ) { profile, projects, animations, _ ->
        if (profile == null) {
            GameUiState.NeedsOnboarding
        } else {
            GameUiState.Ready(
                catName = profile.catName,
                levelInfo = LevelCurve.infoFor(profile.totalXp),
                yarnBalance = profile.yarnBalance,
                sessionsLogged = service.sessionTotal(),
                stitchesRecorded = service.stitchTotal(),
                rewardedSessionsToday = service.rewardedSessionsToday(),
                maxRewardedSessionsPerDay = RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY,
                animationsEnabled = animations,
                projects = projects,
                questBoard = service.questBoard(),
                achievements = service.achievements(),
                wardrobe = service.wardrobe()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState.Loading)

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    private val levelUpChannel = Channel<LevelUpEvent>(Channel.BUFFERED)
    val levelUps: Flow<LevelUpEvent> = levelUpChannel.receiveAsFlow()

    private fun announceLevel(leveledUp: Boolean, level: Int) {
        if (leveledUp) levelUpChannel.trySend(LevelUpEvent(level, LevelCurve.titleForLevel(level)))
    }

    /** Guards against double taps: only one game action runs at a time. */
    private var busy = false

    fun observeProjectUpdates(projectId: Long): Flow<List<ProjectUpdate>> =
        repository.observeProjectUpdates(projectId)

    // ---- profile and sessions ----

    fun completeOnboarding(catName: String) = runExclusive {
        service.createProfile(catName)
    }

    fun logSession(durationMinutes: Int?, newStitchName: String?) = runExclusive {
        val result = service.logSession(durationMinutes, newStitchName)
        if (result != null) {
            announceLevel(result.leveledUp, result.levelAfter)
            messageChannel.send(result.toMessage())
        }
    }

    fun renameCat(newName: String) = runExclusive {
        val ok = service.renameCat(newName)
        messageChannel.send(
            if (ok) "Your cat's name has been updated." else "Please enter a name for your cat."
        )
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setAnimationsEnabled(enabled) }
    }

    // ---- projects ----

    fun saveProject(draft: ProjectDraft, onSaved: () -> Unit) = runExclusive {
        val result = service.saveProject(draft)
        if (result == null) {
            messageChannel.send("Please give your project a name.")
        } else {
            val note = result.checks.suffix()
            messageChannel.send(
                (if (draft.id == null) "Project added." else "Project saved.") + note
            )
            onSaved()
        }
    }

    fun startProject(id: Long) = runExclusive {
        if (service.startProject(id)) messageChannel.send("Project started. Happy stitching!")
    }

    fun setProjectProgress(id: Long, percent: Int) = runExclusive {
        val result = service.setProjectProgress(id, percent)
        if (result != null) {
            announceLevel(result.leveledUp, result.levelAfter)
            messageChannel.send(result.toProgressMessage())
        }
    }

    fun completeProject(id: Long) = runExclusive {
        val result = service.completeProject(id)
        if (result != null) {
            announceLevel(result.leveledUp, result.levelAfter)
            messageChannel.send(result.toCompletionMessage())
        }
    }

    fun reopenProject(id: Long) = runExclusive {
        if (service.reopenProject(id)) {
            messageChannel.send("Project reopened. Rewards are only paid once per project.")
        }
    }

    fun deleteProject(id: Long, onDeleted: () -> Unit) = runExclusive {
        if (service.deleteProject(id)) {
            messageChannel.send("Project deleted.")
            onDeleted()
        }
    }

    // ---- quests ----

    fun claimQuest(questKey: String, periodKey: String) = runExclusive {
        val result = service.claimQuest(questKey, periodKey)
        if (result != null) {
            announceLevel(result.leveledUp, result.levelAfter)
            messageChannel.send(result.toMessage())
        }
    }

    fun markQuestDone(questKey: String) = runExclusive {
        val checks = service.markManualQuestDone(questKey)
        if (checks != null) {
            messageChannel.send("Lovely! Your reward is ready to claim." + checks.suffix())
        }
    }

    // ---- wardrobe ----

    fun purchase(itemKey: String) = runExclusive {
        val name = CosmeticCatalog.find(itemKey)?.name ?: "item"
        messageChannel.send(
            when (service.purchaseItem(itemKey)) {
                PurchaseResult.SUCCESS -> "You bought the $name! Tap Equip to put it on."
                PurchaseResult.NOT_ENOUGH_YARN -> "Not enough yarn balls yet. Keep crocheting!"
                PurchaseResult.ALREADY_OWNED -> "You already own the $name."
                PurchaseResult.UNKNOWN_ITEM, PurchaseResult.NO_PROFILE -> "That item isn't available."
            }
        )
    }

    fun equip(itemKey: String) = runExclusive {
        if (service.equipItem(itemKey)) {
            messageChannel.send("Looking good!")
        }
    }

    fun unequip(slot: AccessorySlot) = runExclusive { service.unequipSlot(slot) }

    fun unequipAll() = runExclusive {
        service.unequipAll()
        messageChannel.send("Back to the classic look.")
    }

    // ---- reset ----

    /** The UI asks for explicit confirmation first. Afterwards the app returns to onboarding. */
    fun resetProgress() = runExclusive {
        service.resetProgress()
    }

    private fun runExclusive(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                messageChannel.send("Something went wrong. Please try again.")
            } finally {
                busy = false
            }
        }
    }
}

// ---------------------------------------------------------------- message text

private fun yarnWord(count: Int) = if (count == 1) "ball" else "balls"

private fun rewardText(xp: Int, yarn: Int) = " +$xp XP, +$yarn yarn ${yarnWord(yarn)}."

private fun levelUpText(leveledUp: Boolean, level: Int) =
    if (leveledUp) " Level up! Level $level · ${LevelCurve.titleForLevel(level)}" else ""

private fun ChecksResult.suffix(): String = buildString {
    completedQuestTitles.forEach { append(" Quest ready to claim: $it.") }
    unlockedAchievementNames.forEach { append(" Achievement unlocked: $it!") }
}

internal fun SessionResult.toMessage(): String = buildString {
    append(
        if (sessionRewarded) "Lovely stitching!"
        else "Session saved. Today's cozy rewards are already collected, so enjoy the crochet!"
    )
    when {
        newStitchName != null -> append(" New stitch discovered: $newStitchName.")
        knownStitchName != null -> append(" You've already recorded $knownStitchName.")
    }
    if (xpEarned > 0 || yarnEarned > 0) append(rewardText(xpEarned, yarnEarned))
    append(levelUpText(leveledUp, levelAfter))
    append(checks.suffix())
}

private fun ProjectResult.toProgressMessage(): String = buildString {
    if (rewardedUpdate) {
        append("Nice progress!").append(rewardText(xpEarned, yarnEarned))
    } else {
        append("Progress saved.")
    }
    append(levelUpText(leveledUp, levelAfter))
    append(checks.suffix())
}

private fun ProjectResult.toCompletionMessage(): String = buildString {
    if (xpEarned > 0 || yarnEarned > 0) {
        append("Project finished! What a lovely make.").append(rewardText(xpEarned, yarnEarned))
    } else {
        append("Project finished again! The completion reward is only paid once per project.")
    }
    append(levelUpText(leveledUp, levelAfter))
    append(checks.suffix())
}

internal fun ClaimResult.toMessage(): String = buildString {
    append("Quest claimed: $questTitle.").append(rewardText(xpEarned, yarnEarned))
    append(levelUpText(leveledUp, levelAfter))
    append(checks.suffix())
}

val GameViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as CrochetQuestApp
        GameViewModel(
            repository = app.container.gameRepository,
            service = app.container.gameService,
            settings = app.container.settingsRepository
        )
    }
}
