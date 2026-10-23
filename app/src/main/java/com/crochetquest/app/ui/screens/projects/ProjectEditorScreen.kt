package com.crochetquest.app.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.projects.ProjectDraft
import com.crochetquest.app.domain.projects.ProjectStatus
import com.crochetquest.app.domain.projects.categoryEnum
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.util.formatEpochDay

private const val MILLIS_PER_DAY = 86_400_000L

/** Add a new project (initial == null) or edit the details of an existing one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorScreen(
    initial: Project?,
    onSave: (ProjectDraft) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = initial == null
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var categoryName by rememberSaveable {
        mutableStateOf((initial?.categoryEnum ?: ProjectCategory.OTHER).name)
    }
    var startNow by rememberSaveable { mutableStateOf(false) }
    var targetDay by rememberSaveable { mutableStateOf(initial?.targetDateEpochDay) }
    var hookSize by rememberSaveable { mutableStateOf(initial?.hookSize ?: "") }
    var materials by rememberSaveable { mutableStateOf(initial?.materialNotes ?: "") }
    var stitchNotes by rememberSaveable { mutableStateOf(initial?.stitchNotes ?: "") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var triedToSave by rememberSaveable { mutableStateOf(false) }

    val nameMissing = name.isBlank()

    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (isNew) "New project" else "Edit project",
                style = MaterialTheme.typography.titleLarge
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= RewardConfig.MAX_PROJECT_NAME_LENGTH) name = it },
            label = { Text("Project name") },
            singleLine = true,
            isError = triedToSave && nameMissing,
            supportingText = {
                if (triedToSave && nameMissing) Text("Please give your project a name")
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (isNew) {
            Text("How's it going?", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !startNow,
                    onClick = { startNow = false },
                    label = { Text(ProjectStatus.PLANNED.label) }
                )
                FilterChip(
                    selected = startNow,
                    onClick = { startNow = true },
                    label = { Text(ProjectStatus.IN_PROGRESS.label) }
                )
            }
        }

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("Category", style = MaterialTheme.typography.titleMedium)
            ProjectCategory.entries.forEach { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = categoryName == category.name,
                            role = Role.RadioButton,
                            onClick = { categoryName = category.name }
                        )
                ) {
                    RadioButton(selected = categoryName == category.name, onClick = null)
                    Text(category.label, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= RewardConfig.MAX_PROJECT_NOTE_LENGTH) description = it },
            label = { Text("Description (optional)") },
            minLines = 2,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        CozyCard(modifier = Modifier.fillMaxWidth()) {
            Text("Target date (optional)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = targetDay?.let { formatEpochDay(it) } ?: "No target date. No pressure.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (targetDay == null) "Pick a date" else "Change date") }
                if (targetDay != null) {
                    TextButton(
                        onClick = { targetDay = null },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Clear") }
                }
            }
        }

        OutlinedTextField(
            value = hookSize,
            onValueChange = { if (it.length <= RewardConfig.MAX_HOOK_SIZE_LENGTH) hookSize = it },
            label = { Text("Hook size (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = materials,
            onValueChange = { if (it.length <= RewardConfig.MAX_PROJECT_NOTE_LENGTH) materials = it },
            label = { Text("Yarn and materials (optional)") },
            supportingText = { Text("Colors, brands, amounts. Noting a new color combo earns a quest.") },
            minLines = 2,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = stitchNotes,
            onValueChange = { if (it.length <= RewardConfig.MAX_PROJECT_NOTE_LENGTH) stitchNotes = it },
            label = { Text("Stitch notes (optional)") },
            minLines = 2,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                triedToSave = true
                if (!nameMissing) {
                    onSave(
                        ProjectDraft(
                            id = initial?.id,
                            name = name,
                            description = description,
                            category = ProjectCategory.valueOf(categoryName),
                            status = if (startNow) ProjectStatus.IN_PROGRESS else ProjectStatus.PLANNED,
                            targetDateEpochDay = targetDay,
                            hookSize = hookSize,
                            materialNotes = materials,
                            stitchNotes = stitchNotes
                        )
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                if (isNew) "Add project" else "Save changes",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDay?.let { it * MILLIS_PER_DAY }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker reports UTC midnight of the chosen day.
                    targetDay = pickerState.selectedDateMillis?.let { it / MILLIS_PER_DAY }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
