package com.crochetquest.app.ui.screens.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.domain.projects.ProjectStatus
import com.crochetquest.app.domain.projects.categoryEnum
import com.crochetquest.app.domain.projects.statusEnum
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites
import com.crochetquest.app.ui.util.daysBetween
import com.crochetquest.app.ui.util.formatEpochDay
import com.crochetquest.app.ui.util.formatMillis

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    val ongoing = projects.filter { it.statusEnum != ProjectStatus.COMPLETED }
    val completed = projects.filter { it.statusEnum == ProjectStatus.COMPLETED }
    val visible = if (showCompleted) completed else ongoing

    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            if (projects.isNotEmpty()) {
                Button(onClick = onAddProject, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add project", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (projects.isEmpty()) {
            EmptyProjects(onAddProject)
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showCompleted,
                onClick = { showCompleted = false },
                label = { Text("In progress (${ongoing.size})") }
            )
            FilterChip(
                selected = showCompleted,
                onClick = { showCompleted = true },
                label = { Text("Completed (${completed.size})") }
            )
        }
        Spacer(Modifier.height(12.dp))

        if (visible.isEmpty()) {
            Text(
                text = if (showCompleted) "Nothing finished yet. Your first finished project will appear here."
                else "No projects in progress. Add one whenever you're ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visible, key = { it.id }) { project ->
                    ProjectCard(project, onClick = { onOpenProject(project.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyProjects(onAddProject: () -> Unit) {
    CozyCard(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PixelSpriteImage(Sprites.yarnBall, Modifier.size(72.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            "No projects yet",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Add what you're making, or plan to make. Only a name is needed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAddProject,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Add Your First Project", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit) {
    CozyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Text(project.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${project.categoryEnum.label} · ${project.statusEnum.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        if (project.statusEnum == ProjectStatus.COMPLETED) {
            val finished = project.completedAt
            Text(
                text = if (finished != null) {
                    "Finished ${formatMillis(finished)} · took ${daysBetween(project.createdAt, finished)} day(s)"
                } else {
                    "Finished"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            ProgressBar(project.progressPercent)
            project.targetDateEpochDay?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Target: ${formatEpochDay(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun ProgressBar(percent: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        LinearProgressIndicator(
            progress = { percent / 100f },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$percent% complete",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
