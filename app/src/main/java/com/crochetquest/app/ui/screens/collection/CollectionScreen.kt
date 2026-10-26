package com.crochetquest.app.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.domain.achievements.AchievementItem
import com.crochetquest.app.domain.cosmetics.WardrobeItem
import com.crochetquest.app.domain.projects.categoryEnum
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.LayeredCat
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites
import com.crochetquest.app.ui.components.TrophySprites
import com.crochetquest.app.ui.util.formatMillis

private const val TROPHIES_PER_SHELF = 4
private val Plank = Color(0xFFC4915F)
private val PlankTop = Color(0xFFD9AE82)

@Composable
fun CollectionScreen(
    achievements: List<AchievementItem>,
    wardrobe: List<WardrobeItem>,
    catName: String,
    finishedProjects: List<Project>,
    onOpenWardrobe: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Column {
                Text("Collection", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Everything you've made and earned, all in one cozy place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "stats-link") {
            OutlinedButton(
                onClick = onOpenStats,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) { Text("Your stats and year in yarn") }
        }

        // ---- trophy shelf ----
        item(key = "trophy-title") {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Trophy shelf", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (finishedProjects.isEmpty()) "Finished projects will sit here as little pixel keepsakes."
                    else "${finishedProjects.size} finished ${if (finishedProjects.size == 1) "project" else "projects"}. Tap one to open it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (finishedProjects.isEmpty()) {
            item(key = "trophy-empty") {
                CozyCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "Your shelf is waiting for its first treasure. Finish a project and it will appear here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            finishedProjects.chunked(TROPHIES_PER_SHELF).forEachIndexed { index, shelf ->
                item(key = "trophy-shelf-$index") { TrophyShelfRow(shelf, onOpenProject) }
            }
        }

        // ---- achievements ----
        item(key = "achievements-title") {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Achievements", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$unlockedCount of ${achievements.size} unlocked. Locked badges are goals to look forward to, nothing to worry about.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(achievements, key = { it.definition.key }) { item ->
            AchievementCard(item)
        }

        // ---- inventory ----
        item(key = "inventory-title") {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Inventory", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Accessories you own: ${wardrobe.count { it.owned }} of ${wardrobe.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val owned = wardrobe.filter { it.owned }
        if (owned.isEmpty()) {
            item(key = "inventory-empty") {
                CozyCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "Nothing yet. Earn yarn balls by crocheting, then visit the wardrobe.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(owned, key = { "owned-${it.item.key}" }) { entry ->
                CozyCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LayeredCat(
                            catName = catName,
                            accessoryKeys = listOf(entry.item.key),
                            modifier = Modifier.size(width = 48.dp, height = 60.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${entry.item.slot.label} · ${if (entry.equipped) "Equipped" else "Owned"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item(key = "open-wardrobe") {
            Button(
                onClick = onOpenWardrobe,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) { Text("Open Wardrobe", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/** One wooden shelf holding up to four keepsakes. */
@Composable
private fun TrophyShelfRow(projects: List<Project>, onOpenProject: (Long) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            projects.forEach { project ->
                TrophyTile(project, onOpenProject, Modifier.weight(1f))
            }
            repeat(TROPHIES_PER_SHELF - projects.size) { Spacer(Modifier.weight(1f)) }
        }
        PlankLine(PlankTop, 3)
        PlankLine(Plank, 8)
    }
}

@Composable
private fun PlankLine(color: Color, heightDp: Int) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .background(color)
    )
}

@Composable
private fun TrophyTile(project: Project, onOpenProject: (Long) -> Unit, modifier: Modifier = Modifier) {
    val finished = project.completedAt?.let { " Finished ${formatMillis(it)}." } ?: ""
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Open ${project.name}", role = Role.Button) { onOpenProject(project.id) }
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        PixelSpriteImage(
            sprite = TrophySprites.forCategory(project.categoryEnum),
            contentDescription = "${project.categoryEnum.label} keepsake.$finished",
            modifier = Modifier.size(52.dp)
        )
        Text(
            project.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AchievementCard(item: AchievementItem) {
    val unlocked = item.isUnlocked
    CozyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (unlocked) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.background
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelSpriteImage(
                sprite = Sprites.forIcon(item.definition.icon),
                silhouette = if (unlocked) null else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.definition.name, style = MaterialTheme.typography.titleMedium)
                Text(item.definition.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = item.unlockedAt?.let { "Unlocked ${formatMillis(it)}" } ?: "Locked · a goal for later",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
