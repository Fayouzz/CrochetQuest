package com.crochetquest.app.ui.screens.home

import android.provider.Settings as AndroidSettings
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.components.BobbingCat
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites

@Composable
fun HomeScreen(
    state: GameUiState.Ready,
    onLogSession: (durationMinutes: Int?, newStitch: String?) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSessionDialog by rememberSaveable { mutableStateOf(false) }
    val animate = rememberAnimationsAllowed(state.animationsEnabled)

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
        CatCard(state, animate)

        Button(
            onClick = { showSessionDialog = true },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text("Log Crochet Session", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = rewardHint(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        StatsCard(state)
    }

    if (showSessionDialog) {
        LogSessionDialog(
            onDismiss = { showSessionDialog = false },
            onConfirm = { minutes, stitch ->
                showSessionDialog = false
                onLogSession(minutes, stitch)
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
private fun CatCard(state: GameUiState.Ready, animate: Boolean) {
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
            BobbingCat(
                animate = animate,
                contentDescription = "${state.catName}, your pixel-art cat companion",
                modifier = Modifier.size(144.dp)
            )
            Spacer(Modifier.width(8.dp))
            PixelSpriteImage(Sprites.yarnBall, Modifier.size(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = state.catName, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Your cozy companion",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsCard(state: GameUiState.Ready) {
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(value = state.sessionsLogged, label = "sessions logged")
            Stat(value = state.stitchesRecorded, label = "stitches discovered")
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@Composable
private fun LogSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (durationMinutes: Int?, newStitch: String?) -> Unit
) {
    var duration by rememberSaveable { mutableStateOf(30) }
    var stitch by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a crochet session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How long did you crochet?", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATION_OPTIONS.forEach { (minutes, label) ->
                        FilterChip(
                            selected = duration == minutes,
                            onClick = { duration = minutes },
                            label = { Text(label) }
                        )
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
            Button(onClick = { onConfirm(duration, stitch.ifBlank { null }) }) {
                Text("Log session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Respects both the in-app toggle and Android's system "remove animations" setting. */
@Composable
private fun rememberAnimationsAllowed(userEnabled: Boolean): Boolean {
    val context = LocalContext.current
    val systemScale = remember {
        AndroidSettings.Global.getFloat(
            context.contentResolver,
            AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    return userEnabled && systemScale > 0f
}
