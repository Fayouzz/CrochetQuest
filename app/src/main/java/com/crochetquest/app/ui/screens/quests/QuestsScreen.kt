package com.crochetquest.app.ui.screens.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.domain.quests.QuestCategory
import com.crochetquest.app.domain.quests.QuestItem
import com.crochetquest.app.domain.quests.QuestRule
import com.crochetquest.app.domain.quests.QuestStatus
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites

@Composable
fun QuestsScreen(
    items: List<QuestItem>,
    onClaim: (questKey: String, periodKey: String) -> Unit,
    onMarkDone: (questKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Column {
                Text("Quest Board", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Optional, cozy goals. No deadlines, no penalties, just little rewards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        QuestCategory.entries.forEach { category ->
            val inCategory = items.filter { it.definition.category == category }
            if (inCategory.isNotEmpty()) {
                item(key = "header-${category.name}") {
                    Text(
                        category.label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(inCategory, key = { it.definition.key }) { quest ->
                    QuestCard(quest, onClaim, onMarkDone)
                }
            }
        }
    }
}

@Composable
fun QuestCard(
    quest: QuestItem,
    onClaim: (questKey: String, periodKey: String) -> Unit,
    onMarkDone: (questKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val definition = quest.definition
    CozyCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = when (quest.status) {
            QuestStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
            QuestStatus.CLAIMED -> MaterialTheme.colorScheme.background
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelSpriteImage(Sprites.forIcon(definition.icon), Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(definition.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${quest.status.label} · ${definition.repeat.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(definition.description, style = MaterialTheme.typography.bodyMedium)

        if (quest.target > 1 && quest.status != QuestStatus.CLAIMED) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { quest.current.toFloat() / quest.target },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Text(
                "${quest.current} of ${quest.target}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            rewardLabel(definition.reward),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            quest.status == QuestStatus.COMPLETED -> {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onClaim(definition.key, quest.periodKey) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) { Text("Claim reward") }
            }
            definition.rule == QuestRule.Manual && quest.status != QuestStatus.CLAIMED -> {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onMarkDone(definition.key) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) { Text("I did this") }
            }
        }
    }
}

private fun rewardLabel(reward: RewardConfig.Reward): String =
    "Reward: ${reward.xp} XP + ${reward.yarn} yarn ${if (reward.yarn == 1) "ball" else "balls"}"
