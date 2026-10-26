package com.crochetquest.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import com.crochetquest.app.domain.cosmetics.RoomAutoDecor
import kotlin.math.floor

private val DayWall = Color(0xFFFDEDEF)
private val DayFloor = Color(0xFFE3C9A6)
private val DayBase = Color(0xFFD8B48C)
private val NightWall = Color(0xFF3A3050)
private val NightFloor = Color(0xFF4A3B4A)
private val NightBase = Color(0xFF2B2240)

/**
 * The cozy room, drawn in one canvas on a 48 x 36 pixel grid so the furniture, the cat and its
 * accessories all share exactly the same pixels. Night shows stars in the window, glowing lamps
 * and a sleeping cat. Free decorations appear automatically as [level] rises.
 */
@Composable
fun RoomScene(
    placedKeys: Set<String>,
    level: Int,
    night: Boolean,
    catName: String,
    accessoryKeys: List<String>,
    modifier: Modifier = Modifier
) {
    val autoKeys = remember(level) { RoomAutoDecor.unlockedAt(level).map { it.key } }
    // Rugs first so everything else sits on top of them.
    val furniture = remember(placedKeys) {
        placedKeys.sortedBy { if (it.startsWith("room_rug")) 0 else 1 }.mapNotNull { RoomSprites.forKey(it) }
    }
    val decor = remember(autoKeys) { autoKeys.mapNotNull { RoomSprites.forKey(it) } }
    val worn = remember(accessoryKeys) {
        accessoryKeys.mapNotNull { CosmeticCatalog.find(it) }
            .sortedBy { it.slot.ordinal }
            .mapNotNull { CosmeticSprites.forItem(it.key) }
    }
    val description = "Your cozy room with $catName, ${furniture.size} furniture pieces and " +
        "${decor.size} free decorations, shown at ${if (night) "night" else "daytime"}"

    Canvas(
        modifier = modifier
            .aspectRatio(RoomSprites.GRID_WIDTH.toFloat() / RoomSprites.GRID_HEIGHT)
            .semantics { contentDescription = description }
    ) {
        val cell = floor(minOf(size.width / RoomSprites.GRID_WIDTH, size.height / RoomSprites.GRID_HEIGHT))
            .coerceAtLeast(1f)
        val left = floor((size.width - cell * RoomSprites.GRID_WIDTH) / 2f)
        val top = floor((size.height - cell * RoomSprites.GRID_HEIGHT) / 2f)

        fun fill(x: Float, y: Float, w: Float, h: Float, color: Color) {
            drawRect(color, Offset(left + x * cell, top + y * cell), Size(w * cell, h * cell))
        }

        val floorY = RoomSprites.FLOOR_Y.toFloat()
        val base = if (night) NightBase else DayBase
        fill(0f, 0f, RoomSprites.GRID_WIDTH.toFloat(), floorY, if (night) NightWall else DayWall)
        fill(0f, floorY, RoomSprites.GRID_WIDTH.toFloat(), RoomSprites.GRID_HEIGHT - floorY, if (night) NightFloor else DayFloor)
        fill(0f, floorY - 1f, RoomSprites.GRID_WIDTH.toFloat(), 1f, base)
        var y = floorY + 2f
        while (y < RoomSprites.GRID_HEIGHT) { // thin floorboard lines
            fill(0f, y, RoomSprites.GRID_WIDTH.toFloat(), 0.25f, base)
            y += 3f
        }

        (decor + furniture).forEach { art ->
            drawSprite(art.sprite(night), left + art.x * cell, top + art.y * cell, cell)
        }

        // The cat, on its own 16 x 20 canvas, then its accessories, then the nap bubbles.
        val catLeft = left + RoomSprites.CAT_X * cell
        val catTop = top + RoomSprites.CAT_Y * cell
        drawSprite(Sprites.cat, catLeft, catTop, cell)
        if (night) drawSprite(CosmeticSprites.blink, catLeft, catTop, cell)
        worn.forEach { drawSprite(it, catLeft, catTop, cell) }
        if (night) drawSprite(CosmeticSprites.zzz, catLeft, catTop, cell)
    }
}
