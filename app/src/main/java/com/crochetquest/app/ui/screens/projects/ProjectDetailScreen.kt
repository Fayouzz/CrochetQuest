package com.crochetquest.app.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.domain.projects.ProjectStatus
import com.crochetquest.app.domain.projects.categoryEnum
import com.crochetquest.app.domain.stash.StashItem
import com.crochetquest.app.domain.projects.statusEnum
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.util.daysBetween
import com.crochetquest.app.ui.util.formatEpochDay
import com.crochetquest.app.ui.util.formatMillis
import kotlin.math.roundToInt

@Composable
fun ProjectDetailScreen(
    project: Project?,
    updates: List<ProjectUpdate>,
    stash: List<StashItem>,
    linkedYarnIds: Set<Long>,
    onSetYarns: (Set<Long>) -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onSetProgress: (Int) -> Unit,
    onFinish: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = project?.name ?: "Project",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        if (project == null) {
            Text("This project could not be found.", style = MaterialTheme.typography.bodyLarge)
            return@Column
        }

        var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
        val status = project.statusEnum

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "${project.categoryEnum.label} · ${status.label}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Started ${formatMillis(project.createdAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            project.targetDateEpochDay?.let {
                Text(
                    "Target: ${formatEpochDay(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (status == ProjectStatus.COMPLETED && project.completedAt != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Finished ${formatMillis(project.completedAt)} after " +
                        "${daysBetween(project.createdAt, project.completedAt)} day(s). Well done!",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (status != ProjectStatus.COMPLETED) {
            ProgressSection(project, onSetProgress)
        }

        ActionsSection(status, onStart, onFinish, onReopen)

        YarnColorsSection(stash, linkedYarnIds, onSetYarns)

        if (status == ProjectStatus.COMPLETED) {
            OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) { Text("Share a card of your cat and this project") }
        }

        DetailsSection(project)

        if (updates.isNotEmpty()) {
            CozyCard(modifier = Modifier.fillMaxWidth()) {
                Text("Progress history", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                updates.take(10).forEach { update ->
                    Text(
                        "${formatMillis(update.loggedAt)}: ${update.fromPercent}% to ${update.toPercent}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Edit details") }
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Delete project") }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete this project?") },
                text = {
                    Text("\"${project.name}\" and its progress history will be removed. This can't be undone.")
                },
                confirmButton = {
                    Button(onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Keep it") }
                }
            )
        }
    }
}

@Composable
private fun ProgressSection(project: Project, onSetProgress: (Int) -> Unit) {
    // Local slider state; the change is saved once, when the finger lifts.
    var sliderValue by remember(project.progressPercent) {
        mutableStateOf(project.progressPercent.toFloat())
    }
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        Text("Progress", style = MaterialTheme.typography.titleMedium)
        Text(
            "${sliderValue.roundToInt()}% complete",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onSetProgress(sliderValue.roundToInt()) },
            valueRange = 0f..100f,
            steps = 19
        )
        if (project.progressPercent >= 100) {
            Text(
                "All the way there! Tap \"Mark as finished\" below when you're done.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionsSection(
    status: ProjectStatus,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onReopen: () -> Unit
) {
    when (status) {
        ProjectStatus.PLANNED -> {
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) { Text("Start this project") }
            OutlinedButton(
                onClick = onFinish,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) { Text("Mark as finished") }
        }
        ProjectStatus.IN_PROGRESS -> Button(
            onClick = onFinish,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) { Text("Mark as finished") }
        ProjectStatus.COMPLETED -> OutlinedButton(
            onClick = onReopen,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) { Text("Reopen project") }
    }
}

@Composable
private fun DetailsSection(project: Project) {
    val rows = listOf(
        "About" to project.description,
        "Hook size" to project.hookSize,
        "Yarn and materials" to project.materialNotes,
        "Stitch notes" to project.stitchNotes
    ).filter { it.second.isNotBlank() }
    if (rows.isEmpty()) return
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The stash colors used in this project, with a picker to change them. */
@Composable
private fun YarnColorsSection(
    stash: List<StashItem>,
    linkedIds: Set<Long>,
    onSetYarns: (Set<Long>) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val linked = stash.filter { it.yarn.id in linkedIds }
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        Text("Yarn colors", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        if (linked.isEmpty()) {
            Text(
                if (stash.isEmpty()) "Add colors in the Yarn stash (Projects tab) to link them here."
                else "No colors linked yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            linked.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Swatch(item.yarn.colorArgb, Modifier.size(24.dp))
                    Text(item.yarn.name, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (stash.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showPicker = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Choose yarn colors") }
        }
    }

    if (showPicker) {
        var chosen by rememberSaveable { mutableStateOf(linkedIds.toList()) }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Yarn colors for this project") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    stash.forEach { item ->
                        val isChosen = item.yarn.id in chosen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(value = isChosen, role = Role.Checkbox) { on ->
                                    chosen = if (on) chosen + item.yarn.id else chosen - item.yarn.id
                                }
                        ) {
                            Checkbox(checked = isChosen, onCheckedChange = null)
                            Swatch(item.yarn.colorArgb, Modifier.padding(start = 8.dp).size(24.dp))
                            Text(item.yarn.name, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPicker = false
                    onSetYarns(chosen.toSet())
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        )
    }
}
