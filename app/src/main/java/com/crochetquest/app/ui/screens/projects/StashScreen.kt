package com.crochetquest.app.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.domain.stash.StashItem
import com.crochetquest.app.domain.stash.StashLimits
import com.crochetquest.app.domain.stash.YarnSwatches
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites

/** The player's yarn colors: a name, a swatch, and how many skeins are left. */
@Composable
fun StashScreen(
    stash: List<StashItem>,
    projects: List<Project>,
    onSave: (id: Long?, name: String, colorArgb: Int, amount: Int) -> Unit,
    onAdjust: (id: Long, delta: Int) -> Unit,
    onDelete: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // null = closed, 0 = adding a new color, other = editing that color's id
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectNames = projects.associate { it.id to it.name }
    val totalSkeins = stash.sumOf { it.yarn.amount }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Yarn stash", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${stash.size} ${if (stash.size == 1) "color" else "colors"} · $totalSkeins " +
                            if (totalSkeins == 1) "skein" else "skeins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = { editing = 0L }, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add yarn color", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        if (stash.isEmpty()) {
            item(key = "empty") {
                CozyCard(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelSpriteImage(Sprites.yarnBall, Modifier.size(64.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Your stash is empty", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Add the yarn colors you own, then link them to your projects.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        items(stash, key = { it.yarn.id }) { item ->
            StashCard(
                item = item,
                projectNames = item.projectIds.mapNotNull { projectNames[it] },
                onEdit = { editing = item.yarn.id },
                onDelete = { confirmDelete = item.yarn.id },
                onAdjust = { delta -> onAdjust(item.yarn.id, delta) }
            )
        }
    }

    editing?.let { id ->
        val existing = stash.firstOrNull { it.yarn.id == id }?.yarn
        StashDialog(
            initialName = existing?.name ?: "",
            initialColor = existing?.colorArgb ?: YarnSwatches.all.first().argb,
            initialAmount = existing?.amount ?: 1,
            isNew = existing == null,
            onDismiss = { editing = null },
            onConfirm = { name, color, amount ->
                editing = null
                onSave(existing?.id, name, color, amount)
            }
        )
    }

    confirmDelete?.let { id ->
        val name = stash.firstOrNull { it.yarn.id == id }?.yarn?.name ?: "this color"
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remove $name?") },
            text = { Text("It will be taken out of your stash and unlinked from any projects. Your projects stay.") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = null
                    onDelete(id)
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep it") } }
        )
    }
}

@Composable
private fun StashCard(
    item: StashItem,
    projectNames: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjust: (Int) -> Unit
) {
    val yarn = item.yarn
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Swatch(yarn.colorArgb, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(yarn.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${yarn.amount} ${if (yarn.amount == 1) "skein" else "skeins"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Stepper(
                onMinus = { onAdjust(-1) },
                onPlus = { onAdjust(1) },
                minusEnabled = yarn.amount > 0,
                plusEnabled = yarn.amount < StashLimits.MAX_AMOUNT,
                label = yarn.name
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (projectNames.isEmpty()) "Not used in a project yet" else "Used in: " + projectNames.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
            TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove") }
        }
    }
}

@Composable
private fun Stepper(
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = onMinus,
            enabled = minusEnabled,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "One fewer skein of $label" }
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(
            onClick = onPlus,
            enabled = plusEnabled,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "One more skein of $label" }
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

/** A round color swatch with a thin outline so pale colors stay visible. */
@Composable
fun Swatch(argb: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(argb), CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

@Composable
private fun StashDialog(
    initialName: String,
    initialColor: Int,
    initialAmount: Int,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorArgb: Int, amount: Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var color by rememberSaveable { mutableStateOf(initialColor) }
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var triedToSave by rememberSaveable { mutableStateOf(false) }
    val swatchName = YarnSwatches.nameFor(color)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add a yarn color" else "Edit yarn color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= StashLimits.MAX_NAME_LENGTH) name = it },
                    label = { Text("Color name") },
                    singleLine = true,
                    isError = triedToSave && name.isBlank(),
                    supportingText = { if (triedToSave && name.isBlank()) Text("Please give it a name") },
                    shape = RoundedCornerShape(16.dp)
                )
                Text("Swatch: ${swatchName ?: "custom"}", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    YarnSwatches.all.forEach { swatch ->
                        val selected = swatch.argb == color
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(swatch.argb), CircleShape)
                                .selectable(selected = selected, role = Role.RadioButton) { color = swatch.argb }
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "${swatch.name}, selected",
                                    tint = Color(0xFF3D3432)
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Skeins: $amount", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Stepper(
                        onMinus = { amount = (amount - 1).coerceAtLeast(0) },
                        onPlus = { amount = (amount + 1).coerceAtMost(StashLimits.MAX_AMOUNT) },
                        minusEnabled = amount > 0,
                        plusEnabled = amount < StashLimits.MAX_AMOUNT,
                        label = "this yarn"
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                triedToSave = true
                if (name.isNotBlank()) onConfirm(name, color, amount)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
