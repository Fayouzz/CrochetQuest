package com.crochetquest.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.crochetquest.app.CrochetQuestApp
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.repository.GameRepository
import com.crochetquest.app.data.repository.SettingsRepository
import com.crochetquest.app.domain.game.GameService
import com.crochetquest.app.domain.game.LevelCurve
import com.crochetquest.app.domain.game.LevelInfo
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.domain.game.SessionResult
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
        val animationsEnabled: Boolean
    ) : GameUiState
}

class GameViewModel(
    repository: GameRepository,
    private val service: GameService,
    private val settings: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<GameUiState> = combine<PlayerProfile?, Int, Int, Boolean, GameUiState>(
        repository.profile,
        repository.sessionCount,
        repository.stitchCount,
        settings.animationsEnabled
    ) { profile, sessions, stitches, animations ->
        if (profile == null) {
            GameUiState.NeedsOnboarding
        } else {
            GameUiState.Ready(
                catName = profile.catName,
                levelInfo = LevelCurve.infoFor(profile.totalXp),
                yarnBalance = profile.yarnBalance,
                sessionsLogged = sessions,
                stitchesRecorded = stitches,
                rewardedSessionsToday = service.rewardedSessionsToday(),
                maxRewardedSessionsPerDay = RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY,
                animationsEnabled = animations
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState.Loading)

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    /** Guards against double taps: only one game action runs at a time. */
    private var busy = false

    fun completeOnboarding(catName: String) = runExclusive {
        service.createProfile(catName)
    }

    fun logSession(durationMinutes: Int?, newStitchName: String?) = runExclusive {
        val result = service.logSession(durationMinutes, newStitchName)
        if (result != null) messageChannel.send(result.toMessage())
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

internal fun SessionResult.toMessage(): String = buildString {
    append(
        if (sessionRewarded) "Lovely stitching!"
        else "Session saved. Today's cozy rewards are already collected, so enjoy the crochet!"
    )
    when {
        newStitchName != null -> append(" New stitch discovered: $newStitchName.")
        knownStitchName != null -> append(" You've already recorded $knownStitchName.")
    }
    if (xpEarned > 0 || yarnEarned > 0) {
        append(" +$xpEarned XP, +$yarnEarned yarn ${if (yarnEarned == 1) "ball" else "balls"}.")
    }
    if (leveledUp) {
        append(" Level up! Level $levelAfter · ${LevelCurve.titleForLevel(levelAfter)}")
    }
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
