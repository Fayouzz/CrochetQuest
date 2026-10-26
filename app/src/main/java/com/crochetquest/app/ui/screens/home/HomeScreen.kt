package com.crochetquest.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.domain.quests.QuestItem
import com.crochetquest.app.domain.quests.QuestStatus
import com.crochetquest.app.domain.stats.formatMinutes
import com.crochetquest.app.domain.time.minutesForTimer
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PettableCat
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites
import com.crochetquest.app.ui.feedback.FeedbackCue
import com.crochetquest.app.ui.screens.projects.ProgressBar
import com.crochetquest.app.ui.util.rememberAnimationsAllowed
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: GameUiState.Ready,
    reactionTick: Int,
    onFeedback: (FeedbackCue) -> Unit,
    onLogSession: (durationMinutes: Int?, newStitch: String?) -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onFinishTimer: (newStitch: String?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenProjects: () -> Unit,
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onClaimQuest: (questKey: String, periodKey: String) -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSessionDialog by rememberSaveable { mutableStateOf(false) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    val animate = rememberAnimationsAllowed(state.animationsEnabled)
    val timerStart = state.timerStartedAt

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Welcome back, little stitcher!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        ProgressCard(state)
        CatCard(state, animate, reactionTick, onFeedback, onOpenWardrobe)

        if (timerStart == null) {
            Button(
                onClick = { showSessionDialog = true },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Text("Log Crochet Session", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onStartTimer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text("Start timer", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = rewardHint(state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TimerCard(
                startedAt = timerStart,
                onFinish = { showFinishDialog = true },
                onCancel = onCancelTimer
            )
        }

        FeaturedQuestCard(state.featuredQuest, onOpenQuests, onClaimQuest)
        ProjectsSummaryCard(state, onOpenProjects, onAddProject, onOpenProject)
        StatsCard(state, onOpenStats)
    }

    if (showSessionDialog) {
        LogSessionDialog(
            timerMinutes = null,
            onDismiss = { showSessionDialog = false },
            onConfirm = { minutes, stitch ->
                showSessionDialog = false
                onLogSession(minutes, stitch)
            }
        )
    }
    if (showFinishDialog && timerStart != null) {
        LogSessionDialog(
            timerMinutes = minutesForTimer(timerStart, System.currentTimeMillis()),
            onDismiss = { showFinishDialog = false },
            onConfirm = { _, stitch ->
                showFinishDialog = false
                onFinishTimer(stitch)
            }
        )
    }
}

@Composable
private fun ProgressCard(state: GameUiState.Ready) {
    val info = state.levelInfo
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Level ${info.level} · ${info.title}",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { info.progress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${info.xpIntoLevel} / ${info.xpNeededForNext} XP to Level ${info.level + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelSpriteImage(Sprites.yarnBall, Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${state.yarnBalance} yarn ${if (state.yarnBalance == 1) "ball" else "balls"}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CatCard(
    state: GameUiState.Ready,
    animate: Boolean,
    reactionTick: Int,
    onFeedback: (FeedbackCue) -> Unit,
    onOpenWardrobe: () -> Unit
) {
    CozyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            PixelSpriteImage(Sprites.plant, Modifier.size(56.dp))
            Spacer(Modifier.width(8.dp))
            // Tap to pet: hearts and a purr, never any rewards.
            PettableCat(
                catName = state.catName,
                accessoryKeys = state.equippedKeys,
                animate = animate,
                asleep = state.isNight,
                reactionTick = reactionTick,
                onPet = { onFeedback(FeedbackCue.PURR) },
                modifier = Modifier.size(width = 128.dp, height = 160.dp)
            )
            Spacer(Modifier.width(8.dp))
            PixelSpriteImage(Sprites.yarnBall, Modifier.size(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = state.catName, style = MaterialTheme.typography.titleLarge)
        Text(
            text = if (state.isNight) "${state.catName} is napping. Shh!" else "Tap to pet. No rewards, just purrs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onOpenWardrobe,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.heightIn(min = 48.dp)
        ) { Text("Open Wardrobe") }
    }
}

/** Shows the running timer. It reads the start time, so it stays right even if the app was closed. */
@Composable
private fun TimerCard(startedAt: Long, onFinish: () -> Unit, onCancel: () -> Unit) {
    val now by rememberNowMillis()
    val elapsedSeconds = ((now - startedAt) / 1000L).coerceAtLeast(0L)
    val clock = "%d:%02d:%02d".format(elapsedSeconds / 3600, (elapsedSeconds / 60) % 60, elapsedSeconds % 60)
    CozyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crocheting right now", style = MaterialTheme.typography.titleMedium)
        Text(clock, style = MaterialTheme.typography.headlineLarge)
        Text(
            "About ${formatMinutes(minutesForTimer(startedAt, now))} so far. Take all the time you like.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onFinish,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) { Text("Finish and log", style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Cancel timer")
        }
    }
}

/** Ticks once a second, but only while it is on screen. */
@Composable
private fun rememberNowMillis(): State<Long> {
    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    return now
}

@Composable
private fun StatsCard(state: GameUiState.Ready, onOpenStats: () -> Unit) {
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(value = state.sessionsLogged.toString(), label = "sessions logged")
            Stat(value = formatMinutes(state.stats.gentle.totalMinutes), label = "time crocheted")
            Stat(value = state.stitchesRecorded.toString(), label = "stitches discovered")
        }
        TextButton(onClick = onOpenStats, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("See your stats and year in yarn")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeaturedQuestCard(
    quest: QuestItem?,
    onOpenQuests: () -> Unit,
    onClaim: (questKey: String, periodKey: String) -> Unit
) {
    CozyCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
        if (quest == null) {
            Text("Today's cozy quest", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "You've claimed every quest for now. Rest your hands, new ones appear soon.",
                style = MaterialTheme.typography.bodyMedium
            )
            return@CozyCard
        }
        val definition = quest.definition
        Text("Today's cozy quest", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelSpriteImage(Sprites.forIcon(definition.icon), Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(definition.title, style = MaterialTheme.typography.titleMedium)
                Text(definition.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Reward: ${definition.reward.xp} XP + ${definition.reward.yarn} yarn balls · ${quest.status.label}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (quest.status == QuestStatus.COMPLETED) {
                Button(
                    onClick = { onClaim(definition.key, quest.periodKey) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Claim reward") }
            }
            OutlinedButton(
                onClick = onOpenQuests,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("View Quest") }
        }
    }
}

@Composable
private fun ProjectsSummaryCard(
    state: GameUiState.Ready,
    onOpenProjects: () -> Unit,
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit
) {
    val ongoing = state.ongoingProjects
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        Text("Your projects", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (ongoing.isEmpty()) {
            Text(
                if (state.projects.isEmpty()) "Nothing on the hook yet. Add what you're making, or planning to make."
                else "All your projects are finished. Time for something new?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddProject,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (state.projects.isEmpty()) "Add Your First Project" else "Add a project")
            }
        } else {
            ongoing.take(3).forEach { project ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { onOpenProject(project.id) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    ProgressBar(project.progressPercent)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenProjects, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (ongoing.size > 3) "See all ${ongoing.size} projects" else "Open projects")
            }
        }
    }
}

private fun rewardHint(state: GameUiState.Ready): String =
    if (state.rewardedSessionsToday >= state.maxRewardedSessionsPerDay) {
        "You've collected all of today's cozy rewards. More sessions are still saved!"
    } else {
        "Today's cozy rewards: ${state.rewardedSessionsToday} of ${state.maxRewardedSessionsPerDay} collected. " +
            "No pressure, log whenever you like."
    }

private val DURATION_OPTIONS = listOf(15 to "15 min", 30 to "30 min", 60 to "1 hour+")

/**
 * Logs a session. Pass [timerMinutes] when finishing the timer (the duration is then fixed),
 * or null to choose 15, 30 or 60 minutes by hand.
 */
@Composable
private fun LogSessionDialog(
    timerMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (durationMinutes: Int?, newStitch: String?) -> Unit
) {
    var duration by rememberSaveable { mutableStateOf(30) }
    var stitch by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (timerMinutes != null) "Finish your session" else "Log a crochet session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (timerMinutes != null) {
                    Text(
                        "Timer: ${formatMinutes(timerMinutes)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text("How long did you crochet?", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        DURATION_OPTIONS.forEach { (minutes, label) ->
                            FilterChip(
                                selected = duration == minutes,
                                onClick = { duration = minutes },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = stitch,
                    onValueChange = { if (it.length <= RewardConfig.MAX_STITCH_NAME_LENGTH) stitch = it },
                    label = { Text("Tried a new stitch? (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(if (timerMinutes != null) timerMinutes else duration, stitch.ifBlank { null }) }) {
                Text(if (timerMinutes != null) "Log it" else "Log session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
