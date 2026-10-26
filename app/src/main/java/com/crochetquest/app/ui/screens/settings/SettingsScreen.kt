package com.crochetquest.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.crochetquest.app.BuildConfig
import com.crochetquest.app.data.repository.ThemeMode
import com.crochetquest.app.domain.backup.BackupFormat
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.RestorePreview
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.util.formatMillis
import com.crochetquest.app.ui.util.readBackupText
import com.crochetquest.app.ui.util.writeBackupBytes
import java.time.LocalDate

@Composable
fun SettingsScreen(
    state: GameUiState.Ready,
    onBack: () -> Unit,
    onRenameCat: (String) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onResetProgress: () -> Unit,
    onExport: (BackupFormat, suspend (ByteArray) -> Unit) -> Unit,
    onRestoreFile: (suspend () -> String) -> Unit,
    restorePreview: RestorePreview?,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val resolver = LocalContext.current.contentResolver

    // The system file picker does the saving and opening, so the app needs no storage permission.
    val saveJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFormat.JSON.mimeType)
    ) { uri ->
        if (uri != null) onExport(BackupFormat.JSON) { bytes -> resolver.writeBackupBytes(uri, bytes) }
    }
    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFormat.CSV.mimeType)
    ) { uri ->
        if (uri != null) onExport(BackupFormat.CSV) { bytes -> resolver.writeBackupBytes(uri, bytes) }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onRestoreFile { resolver.readBackupText(uri) }
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("Your cat", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            var name by rememberSaveable(state.catName) { mutableStateOf(state.catName) }
            val canSave = name.isNotBlank() && name.trim() != state.catName
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= RewardConfig.MAX_CAT_NAME_LENGTH) name = it },
                label = { Text("Cat's name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onRenameCat(name) }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onRenameCat(name) },
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Save name")
            }
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = state.animationsEnabled,
                        role = Role.Switch,
                        onValueChange = onAnimationsChange
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gentle animations", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A slow bob for your cat. Android's system setting to remove animations is always respected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.animationsEnabled, onCheckedChange = null)
            }
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = state.soundEnabled,
                        role = Role.Switch,
                        onValueChange = onSoundChange
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sounds and haptics", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Soft purrs, stitch clicks and a gentle chime, with a light buzz. Off means silent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.soundEnabled, onCheckedChange = null)
            }
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("Look", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cozy night is a softer, darker theme for evenings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("Backup and restore", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Save everything to a file you choose, and bring it back any time. JSON restores " +
                    "perfectly; CSV opens in a spreadsheet and restores too. Nothing leaves your phone unless you move the file yourself.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            val today = LocalDate.now()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { saveJson.launch("crochet-quest-backup-$today.json") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Save JSON") }
                OutlinedButton(
                    onClick = { saveCsv.launch("crochet-quest-backup-$today.csv") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Save CSV") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { openBackup.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Restore from a backup file") }
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("Start over", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Delete all progress, projects, quests, achievements and accessories, and begin again.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Reset game progress")
            }
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Everything in Crochet Quest is stored only on this device. There are no accounts, " +
                    "no ads and no analytics, and the app works fully offline. Backup files and share " +
                    "cards are only made when you ask, and go only where you choose to send them.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Crochet Quest version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (restorePreview != null) {
            AlertDialog(
                onDismissRequest = onCancelRestore,
                title = { Text("Restore this backup?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        restorePreview.parsed.exportedAt?.let {
                            Text("Saved on ${formatMillis(it)} as ${restorePreview.parsed.format.label}.")
                        }
                        restorePreview.summary.forEach { Text(it) }
                        Text(
                            "Restoring replaces everything in the app right now. This can't be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = { Button(onClick = onConfirmRestore) { Text("Yes, restore") } },
                dismissButton = { TextButton(onClick = onCancelRestore) { Text("Keep my current game") } }
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset all progress?") },
                text = {
                    Text(
                        "This permanently deletes your XP, yarn balls, projects, quests, achievements " +
                            "and accessories, and returns to the welcome screen. It can't be undone."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showResetDialog = false
                        onResetProgress()
                    }) { Text("Yes, reset everything") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Keep my progress") }
                }
            )
        }
    }
}
