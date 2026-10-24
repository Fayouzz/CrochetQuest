package com.crochetquest.app.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.achievements.AchievementItem
import com.crochetquest.app.domain.cosmetics.WardrobeItem
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.LayeredCat
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites
import com.crochetquest.app.ui.util.formatMillis

private val LockedSilhouette = Color(0xFFD9CFC9)

@Composable
fun CollectionScreen(
    achievements: List<AchievementItem>,
    wardrobe: List<WardrobeItem>,
    catName: String,
    onOpenWardrobe: () -> Unit,
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
                    "Achievements: $unlockedCount of ${achievements.size} unlocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Locked badges are goals to look forward to, nothing to worry about.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(achievements, key = { it.definition.key }) { item ->
            AchievementCard(item)
        }
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
                silhouette = if (unlocked) null else LockedSilhouette,
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
