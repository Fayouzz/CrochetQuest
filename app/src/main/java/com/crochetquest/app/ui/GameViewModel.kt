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
import com.crochetquest.app.data.repository.AppSettings
import com.crochetquest.app.data.repository.GameRepository
import com.crochetquest.app.data.repository.SettingsRepository
import com.crochetquest.app.data.repository.ThemeMode
import com.crochetquest.app.domain.achievements.AchievementItem
import com.crochetquest.app.domain.backup.BackupCodec
import com.crochetquest.app.domain.backup.BackupFormat
import com.crochetquest.app.domain.backup.BackupFormatException
import com.crochetquest.app.domain.backup.ParsedBackup
import com.crochetquest.app.domain.cosmetics.AccessorySlot
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import com.crochetquest.app.domain.cosmetics.PurchaseResult
import com.crochetquest.app.domain.cosmetics.RoomCatalog
import com.crochetquest.app.domain.cosmetics.RoomSlot
import com.crochetquest.app.domain.cosmetics.RoomState
import com.crochetquest.app.domain.cosmetics.WardrobeItem
import com.crochetquest.app.domain.game.ChecksResult
import com.crochetquest.app.domain.game.ClaimResult
import com.crochetquest.app.domain.game.GameClock
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
import com.crochetquest.app.domain.stash.StashItem
import com.crochetquest.app.domain.stats.StatsBundle
import com.crochetquest.app.domain.time.DayCycle
import com.crochetquest.app.domain.time.minutesForTimer
import com.crochetquest.app.ui.feedback.FeedbackCue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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
        val settings: AppSettings,
        /** True between 8 pm and 7 am: the cat naps and the room window shows the moon. */
        val isNight: Boolean,
        val projects: List<Project>,
        val questBoard: List<QuestItem>,
        val achievements: List<AchievementItem>,
        val wardrobe: List<WardrobeItem>,
        val room: RoomState,
        val stash: List<StashItem>,
        val stats: StatsBundle
    ) : GameUiState {
        val animationsEnabled: Boolean get() = settings.animationsEnabled
        val soundEnabled: Boolean get() = settings.soundEnabled
        val themeMode: ThemeMode get() = settings.themeMode
        val timerStartedAt: Long? get() = settings.timerStartedAt
        val ongoingProjects: List<Project> get() = projects.filter { it.statusEnum != ProjectStatus.COMPLETED }
        val finishedProjects: List<Project> get() = projects.filter { it.statusEnum == ProjectStatus.COMPLETED }
        val featuredQuest: QuestItem? get() = pickFeaturedQuest(questBoard)
        /** Keys of the accessories the cat is wearing right now. */
        val equippedKeys: List<String> get() = wardrobe.filter { it.equipped }.map { it.item.key }
    }
}

/** Fired when XP crosses a level threshold; drives the short celebration. */
data class LevelUpEvent(val level: Int, val title: String)

/** A backup file that was read and checked, waiting for the player to confirm the restore. */
data class RestorePreview(val parsed: ParsedBackup, val summary: List<String>)

class GameViewModel(
    private val repository: GameRepository,
    private val service: GameService,
    private val settings: SettingsRepository,
    private val clock: GameClock
) : ViewModel() {

    /**
     * Emits at start and then once at each local midnight, 7 am and 8 pm, so daily/weekly quests
     * roll over and the cat falls asleep or wakes up while the app stays open. It sleeps in
     * between (no polling). It only runs while the screen is being observed.
     */
    private val clockTick: Flow<Long> = flow {
        while (true) {
            val now = clock.nowMillis()
            emit(now)
            delay(DayCycle.nextBoundary(now, clock.zone) - now + 1_000L)
        }
    }

    /** Emits whenever any stored game data changes, so derived state is rebuilt. */
    private val dataChanged: Flow<Unit> = combine(
        listOf<Flow<Any?>>(
            repository.sessionCount,
            repository.stitchCount,
            repository.projectUpdateCount,
            repository.questCompletions,
            repository.achievementUnlocks,
            repository.inventory,
            repository.equipped,
            repository.stash,
            repository.projectYarnLinks
        )
    ) { }

    val uiState: StateFlow<GameUiState> =
        combine<PlayerProfile?, List<Project>, AppSettings, Unit, Long, GameUiState>(
            repository.profile,
            repository.projects,
            settings.settings,
            dataChanged,
            clockTick
        ) { profile, projects, appSettings, _, now ->
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
                    settings = appSettings,
                    isNight = DayCycle.isNight(now, clock.zone),
                    projects = projects,
                    questBoard = service.questBoard(),
                    achievements = service.achievements(),
                    wardrobe = service.wardrobe(),
                    room = service.roomState(),
                    stash = service.stash(),
                    stats = service.statsBundle()
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState.Loading)

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    private val levelUpChannel = Channel<LevelUpEvent>(Channel.BUFFERED)
    val levelUps: Flow<LevelUpEvent> = levelUpChannel.receiveAsFlow()

    private val cueChannel = Channel<FeedbackCue>(Channel.BUFFERED)
    /** Sounds and haptics to play (the screen only plays them when the toggle is on). */
    val cues: Flow<FeedbackCue> = cueChannel.receiveAsFlow()

    private val reactionChannel = Channel<Unit>(Channel.BUFFERED)
    /** Each emission makes the cat do a happy hop with a few hearts. */
    val catReactions: Flow<Unit> = reactionChannel.receiveAsFlow()

    private val _restorePreview = MutableStateFlow<RestorePreview?>(null)
    val restorePreview: StateFlow<RestorePreview?> = _restorePreview.asStateFlow()

    /** Guards against double taps: only one game action runs at a time. */
    private var busy = false

    private fun cue(cue: FeedbackCue) {
        cueChannel.trySend(cue)
    }

    private fun announceLevel(leveledUp: Boolean, level: Int) {
        if (leveledUp) levelUpChannel.trySend(LevelUpEvent(level, LevelCurve.titleForLevel(level)))
    }

    fun observeProjectUpdates(projectId: Long): Flow<List<ProjectUpdate>> =
        repository.observeProjectUpdates(projectId)

    // ---- profile and settings ----

    fun completeOnboarding(catName: String) = runExclusive {
        service.createProfile(catName)
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

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setSoundEnabled(enabled) }
        if (enabled) cue(FeedbackCue.CLICK) // a tiny confirmation (played only if now enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    // ---- sessions and the timer ----

    fun logSession(durationMinutes: Int?, newStitchName: String?) = runExclusive {
        val result = service.logSession(durationMinutes, newStitchName)
        if (result != null) afterSession(result)
    }

    fun startTimer() = runExclusive {
        if (settings.current().timerStartedAt == null) {
            settings.startTimer(clock.nowMillis())
            cue(FeedbackCue.CLICK)
            messageChannel.send("Timer started. Happy stitching!")
        }
    }

    fun cancelTimer() = runExclusive {
        settings.clearTimer()
        messageChannel.send("Timer stopped. Nothing was logged.")
    }

    /** Logs the timed session (rounded to the nearest minute) and clears the timer. */
    fun finishTimer(newStitchName: String?) = runExclusive {
        val started = settings.current().timerStartedAt
        if (started != null) {
            val minutes = minutesForTimer(started, clock.nowMillis())
            val result = service.logSession(minutes, newStitchName)
            settings.clearTimer()
            if (result != null) afterSession(result)
        }
    }

    private suspend fun afterSession(result: SessionResult) {
        announceLevel(result.leveledUp, result.levelAfter)
        cue(FeedbackCue.CHIME)
        reactionChannel.trySend(Unit)
        messageChannel.send(result.toMessage())
    }

    // ---- projects ----

    fun saveProject(draft: ProjectDraft, onSaved: () -> Unit) = runExclusive {
        val result = service.saveProject(draft)
        if (result == null) {
            messageChannel.send("Please give your project a name.")
        } else {
            cue(FeedbackCue.CLICK)
            messageChannel.send(
                (if (draft.id == null) "Project added." else "Project saved.") + result.checks.suffix()
            )
            onSaved()
        }
    }

    fun startProject(id: Long) = runExclusive {
        if (service.startProject(id)) {
            cue(FeedbackCue.CLICK)
            messageChannel.send("Project started. Happy stitching!")
        }
    }

    fun setProjectProgress(id: Long, percent: Int) = runExclusive {
        val result = service.setProjectProgress(id, percent)
        if (result != null) {
            announceLevel(result.leveledUp, result.levelAfter)
            cue(if (result.rewardedUpdate) FeedbackCue.CHIME else FeedbackCue.CLICK)
            messageChannel.send(result.toProgressMessage())
        }
    }

    fun completeProject(id: Long) = runExclusive {
        val result = service.completeProject(id)
        if (result != null) {
            announceLevel(result.leveledUp, result.levelAfter)
            cue(FeedbackCue.CHIME)
            reactionChannel.trySend(Unit)
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

    // ---- yarn stash ----

    fun saveStash(id: Long?, name: String, colorArgb: Int, amount: Int) = runExclusive {
        val saved = service.saveStashYarn(id, name, colorArgb, amount)
        if (saved == null) {
            messageChannel.send("Please give the yarn color a name.")
        } else {
            cue(FeedbackCue.CLICK)
            messageChannel.send(if (id == null) "Yarn color added." else "Yarn color saved.")
        }
    }

    fun adjustStash(id: Long, delta: Int) = runExclusive {
        if (service.adjustStashAmount(id, delta)) cue(FeedbackCue.CLICK)
    }

    fun deleteStash(id: Long) = runExclusive {
        if (service.deleteStashYarn(id)) messageChannel.send("Yarn color removed.")
    }

    fun setProjectYarns(projectId: Long, yarnIds: Set<Long>) = runExclusive {
        val checks = service.setProjectYarns(projectId, yarnIds)
        if (checks != null) {
            cue(FeedbackCue.CLICK)
            messageChannel.send("Yarn colors updated." + checks.suffix())
        }
    }

    // ---- quests ----

    fun claimQuest(questKey: String, periodKey: String) = runExclusive {
        val result = service.claimQuest(questKey, periodKey)
        if (result != null) {
            announceLevel(result.leveledUp, result.levelAfter)
            cue(FeedbackCue.CHIME)
            messageChannel.send(result.toMessage())
        }
    }

    fun markQuestDone(questKey: String) = runExclusive {
        val checks = service.markManualQuestDone(questKey)
        if (checks != null) {
            cue(FeedbackCue.CLICK)
            messageChannel.send("Lovely! Your reward is ready to claim." + checks.suffix())
        }
    }

    // ---- wardrobe and room ----

    fun purchase(itemKey: String) = runExclusive {
        val name = CosmeticCatalog.find(itemKey)?.name ?: "item"
        val result = service.purchaseItem(itemKey)
        if (result == PurchaseResult.SUCCESS) cue(FeedbackCue.CHIME)
        messageChannel.send(purchaseMessage(result, "You bought the $name! Tap Equip to put it on."))
    }

    fun equip(itemKey: String) = runExclusive {
        if (service.equipItem(itemKey)) {
            cue(FeedbackCue.CLICK)
            messageChannel.send("Looking good!")
        }
    }

    fun unequip(slot: AccessorySlot) = runExclusive {
        if (service.unequipSlot(slot)) cue(FeedbackCue.CLICK)
    }

    fun unequipAll() = runExclusive {
        service.unequipAll()
        cue(FeedbackCue.CLICK)
        messageChannel.send("Back to the classic look.")
    }

    fun purchaseRoom(itemKey: String) = runExclusive {
        val name = RoomCatalog.find(itemKey)?.name ?: "item"
        val result = service.purchaseRoomItem(itemKey)
        if (result == PurchaseResult.SUCCESS) cue(FeedbackCue.CHIME)
        messageChannel.send(purchaseMessage(result, "You bought the $name! Tap Place to put it in your room."))
    }

    fun placeRoom(itemKey: String) = runExclusive {
        if (service.placeRoomItem(itemKey)) {
            cue(FeedbackCue.CLICK)
            messageChannel.send("Your room feels cozier already.")
        }
    }

    fun removeRoom(slot: RoomSlot) = runExclusive {
        if (service.removeRoomSlot(slot)) cue(FeedbackCue.CLICK)
    }

    private fun purchaseMessage(result: PurchaseResult, success: String): String = when (result) {
        PurchaseResult.SUCCESS -> success
        PurchaseResult.NOT_ENOUGH_YARN -> "Not enough yarn balls yet. Keep crocheting!"
        PurchaseResult.ALREADY_OWNED -> "You already own that."
        PurchaseResult.LEVEL_TOO_LOW -> "You'll unlock that as you level up. Keep going!"
        PurchaseResult.UNKNOWN_ITEM, PurchaseResult.NO_PROFILE -> "That item isn't available."
    }

    // ---- backup and restore ----

    /** Builds the backup file contents and hands them to [write] (which saves them where the player chose). */
    fun exportBackup(format: BackupFormat, write: suspend (ByteArray) -> Unit) = runExclusive {
        val snapshot = service.createBackup()
        val bytes = withContext(Dispatchers.Default) {
            BackupCodec.encode(snapshot, format, clock.nowMillis()).toByteArray(Charsets.UTF_8)
        }
        try {
            withContext(Dispatchers.IO) { write(bytes) }
            messageChannel.send("Backup saved as ${format.label}.")
        } catch (e: IOException) {
            messageChannel.send("Couldn't save the backup file. Try a different location.")
        }
    }

    /** Reads and checks a backup file. Nothing changes until the player confirms. */
    fun prepareRestore(read: suspend () -> String) = runExclusive {
        try {
            val text = withContext(Dispatchers.IO) { read() }
            val parsed = withContext(Dispatchers.Default) { BackupCodec.decode(text) }
            _restorePreview.value = RestorePreview(parsed, BackupCodec.summaryLines(parsed.snapshot))
        } catch (e: BackupFormatException) {
            messageChannel.send(e.message ?: "That file couldn't be used as a backup.")
        } catch (e: IOException) {
            messageChannel.send("Couldn't read that file.")
        }
    }

    fun cancelRestore() {
        _restorePreview.value = null
    }

    fun confirmRestore() = runExclusive {
        val preview = _restorePreview.value ?: return@runExclusive
        _restorePreview.value = null
        service.restoreBackup(preview.parsed.snapshot)
        settings.clearTimer()
        cue(FeedbackCue.CHIME)
        messageChannel.send("Backup restored. Welcome back!")
    }

    // ---- reset ----

    /** The UI asks for explicit confirmation first. Afterwards the app returns to onboarding. */
    fun resetProgress() = runExclusive {
        service.resetProgress()
        settings.clearTimer()
    }

    private fun runExclusive(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupFormatException) {
                messageChannel.send(e.message ?: "That backup couldn't be used.")
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
            settings = app.container.settingsRepository,
            clock = app.container.gameClock
        )
    }
}
