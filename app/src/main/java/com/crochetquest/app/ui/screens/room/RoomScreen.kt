package com.crochetquest.app.ui.screens.room

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
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.cosmetics.RoomAutoDecor
import com.crochetquest.app.domain.cosmetics.RoomEntry
import com.crochetquest.app.domain.cosmetics.RoomSlot
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.RoomScene
import com.crochetquest.app.ui.components.Sprites

/** The cozy room: see it at the top, decorate it below. Items unlock as you level up. */
@Composable
fun RoomScreen(
    state: GameUiState.Ready,
    onBuy: (itemKey: String) -> Unit,
    onPlace: (itemKey: String) -> Unit,
    onRemove: (RoomSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = state.room.level
    val nextFree = RoomAutoDecor.nextAfter(level)

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Column {
                Text("Cozy Room", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Spend yarn balls on little comforts. The room fills up as you level.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "scene") {
            Column {
                RoomScene(
                    placedKeys = state.room.placedKeys,
                    level = level,
                    night = state.isNight,
                    catName = state.catName,
                    accessoryKeys = state.equippedKeys,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelSpriteImage(Sprites.yarnBall, Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.yarnBalance} yarn ${if (state.yarnBalance == 1) "ball" else "balls"} · Level $level",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = nextFree?.let { "Free decoration at level ${it.level}: ${it.name}" }
                        ?: "Every free decoration has appeared. What a lovely room!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RoomSlot.entries.forEach { slot ->
            val entries = state.room.entries.filter { it.item.slot == slot }
            item(key = "header-${slot.name}") {
                Text(
                    slot.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(entries, key = { it.item.key }) { entry ->
                RoomItemCard(
                    entry = entry,
                    yarnBalance = state.yarnBalance,
                    onBuy = { onBuy(entry.item.key) },
                    onPlace = { onPlace(entry.item.key) },
                    onRemove = { onRemove(entry.item.slot) }
                )
            }
        }
    }
}

@Composable
private fun RoomItemCard(
    entry: RoomEntry,
    yarnBalance: Int,
    onBuy: () -> Unit,
    onPlace: () -> Unit,
    onRemove: () -> Unit
) {
    val item = entry.item
    val affordable = yarnBalance >= item.cost
    val status = when {
        entry.placed -> "In your room"
        entry.owned -> "Owned"
        !entry.unlocked -> "Locked · unlocks at level ${item.requiredLevel}"
        else -> "${item.cost} yarn balls"
    }
    CozyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (entry.placed) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    ) {
        Text(item.name, style = MaterialTheme.typography.titleMedium)
        Text(item.description, style = MaterialTheme.typography.bodyMedium)
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        when {
            entry.placed -> OutlinedButton(
                onClick = onRemove,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Remove") }
            entry.owned -> Button(
                onClick = onPlace,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Place") }
            else -> {
                Button(
                    onClick = onBuy,
                    enabled = entry.unlocked && affordable,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Buy for ${item.cost}") }
                if (entry.unlocked && !affordable) {
                    Text(
                        "Need ${item.cost - yarnBalance} more yarn balls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
