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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.achievements.AchievementItem
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites
import com.crochetquest.app.ui.util.formatMillis

private val LockedSilhouette = Color(0xFFD9CFC9)

@Composable
fun CollectionScreen(achievements: List<AchievementItem>, modifier: Modifier = Modifier) {
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
        item(key = "inventory") {
            CozyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Inventory", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your cat's wardrobe and accessories arrive in the next update. " +
                        "Keep collecting yarn balls!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
