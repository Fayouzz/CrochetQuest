package com.crochetquest.app.ui.screens.wardrobe

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.cosmetics.AccessorySlot
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import com.crochetquest.app.domain.cosmetics.WardrobeItem
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.LayeredCat
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites

@Composable
fun WardrobeScreen(
    state: GameUiState.Ready,
    onBack: () -> Unit,
    onBuy: (itemKey: String) -> Unit,
    onEquip: (itemKey: String) -> Unit,
    onUnequip: (AccessorySlot) -> Unit,
    onUnequipAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Preview: try an item on without owning or equipping it.
    var previewKey by rememberSaveable { mutableStateOf<String?>(null) }
    val previewItem = previewKey?.let { CosmeticCatalog.find(it) }
    val worn = state.wardrobe.filter { it.equipped }.map { it.item }
    val shown = (worn.filter { previewItem == null || it.slot != previewItem.slot } +
        listOfNotNull(previewItem)).map { it.key }

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Wardrobe",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "preview") {
            CozyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LayeredCat(
                    catName = state.catName,
                    accessoryKeys = shown,
                    modifier = Modifier.size(width = 144.dp, height = 180.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelSpriteImage(Sprites.yarnBall, Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.yarnBalance} yarn ${if (state.yarnBalance == 1) "ball" else "balls"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (previewItem != null) {
                    val entry = state.wardrobe.firstOrNull { it.item.key == previewItem.key }
                    Text(
                        text = "Previewing: ${previewItem.name}" +
                            if (entry?.owned == true) "" else " (not owned yet)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = { previewKey = null },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Stop previewing") }
                } else {
                    Text(
                        "Tap an accessory to try it on.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item(key = "default") {
            CozyCard(modifier = Modifier.fillMaxWidth()) {
                Text("Default appearance", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (worn.isEmpty()) "Currently wearing nothing extra." else "Take everything off at once.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (worn.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            previewKey = null
                            onUnequipAll()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Remove all accessories") }
                }
            }
        }
        items(state.wardrobe, key = { it.item.key }) { entry ->
            WardrobeCard(
                entry = entry,
                yarnBalance = state.yarnBalance,
                catName = state.catName,
                onPreview = { previewKey = entry.item.key },
                onBuy = { onBuy(entry.item.key) },
                onEquip = {
                    previewKey = null
                    onEquip(entry.item.key)
                },
                onUnequip = { onUnequip(entry.item.slot) }
            )
        }
    }
}

@Composable
private fun WardrobeCard(
    entry: WardrobeItem,
    yarnBalance: Int,
    catName: String,
    onPreview: () -> Unit,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit
) {
    val item = entry.item
    val affordable = yarnBalance >= item.cost
    val status = when {
        entry.equipped -> "Equipped"
        entry.owned -> "Owned"
        else -> "Locked"
    }
    CozyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClickLabel = "Try on ${item.name}", role = Role.Button, onClick = onPreview),
        containerColor = if (entry.equipped) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LayeredCat(
                catName = catName,
                accessoryKeys = listOf(item.key),
                modifier = Modifier.size(width = 56.dp, height = 70.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${item.slot.label} · $status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                if (!entry.owned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelSpriteImage(Sprites.yarnBall, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${item.cost} yarn balls",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            entry.equipped -> OutlinedButton(
                onClick = onUnequip,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Take off") }
            entry.owned -> Button(
                onClick = onEquip,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Equip") }
            else -> {
                Button(
                    onClick = onBuy,
                    enabled = affordable,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Buy for ${item.cost}") }
                if (!affordable) {
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
