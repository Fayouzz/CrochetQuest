package com.crochetquest.app.ui.screens.settings

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.crochetquest.app.BuildConfig
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.components.CozyCard

@Composable
fun SettingsScreen(
    state: GameUiState.Ready,
    onBack: () -> Unit,
    onRenameCat: (String) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
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
                    "no ads and no analytics, and the app works fully offline. Nothing you enter " +
                    "is ever sent anywhere.",
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
