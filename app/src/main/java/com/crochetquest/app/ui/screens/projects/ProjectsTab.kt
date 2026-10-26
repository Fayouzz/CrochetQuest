package com.crochetquest.app.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crochetquest.app.ui.GameUiState

/** The Projects tab: switch between your projects and your yarn stash. */
@Composable
fun ProjectsTab(
    state: GameUiState.Ready,
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onSaveStash: (id: Long?, name: String, colorArgb: Int, amount: Int) -> Unit,
    onAdjustStash: (id: Long, delta: Int) -> Unit,
    onDeleteStash: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStash by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
        ) {
            FilterChip(
                selected = !showStash,
                onClick = { showStash = false },
                label = { Text("Projects") }
            )
            FilterChip(
                selected = showStash,
                onClick = { showStash = true },
                label = { Text("Yarn stash") }
            )
        }
        if (showStash) {
            StashScreen(
                stash = state.stash,
                projects = state.projects,
                onSave = onSaveStash,
                onAdjust = onAdjustStash,
                onDelete = onDeleteStash,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        } else {
            ProjectsScreen(
                projects = state.projects,
                onAddProject = onAddProject,
                onOpenProject = onOpenProject
            )
        }
    }
}
