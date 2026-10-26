package com.crochetquest.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.crochetquest.app.domain.quests.IconKey
import com.crochetquest.app.ui.theme.CozyPalette
import kotlin.math.floor

/** A tiny pixel-art image described by rows of characters mapped through a palette. */
class PixelSprite(val rows: List<String>, private val palette: Map<Char, Color>) {
    val width: Int = rows.maxOf { it.length }
    val height: Int = rows.size

    fun colorAt(char: Char): Color? = palette[char]

    companion object {
        /** Builds a symmetric sprite from its left half (each row mirrored to the right). */
        fun mirrored(halfRows: List<String>, palette: Map<Char, Color>): PixelSprite {
            require(halfRows.isNotEmpty()) { "sprite needs rows" }
            val half = halfRows.first().length
            require(halfRows.all { it.length == half }) { "all sprite rows must have equal length" }
            return PixelSprite(halfRows.map { it + it.reversed() }, palette)
        }
    }
}

/** Every cat sprite and accessory layer is exactly this many pixels wide and tall. */
object CatCanvas {
    const val WIDTH = 16
    const val HEIGHT = 20
}

object Sprites {
    private val catPalette = mapOf(
        'o' to CozyPalette.WarmBrown,
        'c' to Color(0xFFF6D3A8),
        'p' to CozyPalette.DustyRose,
        'e' to CozyPalette.DarkText,
        'w' to CozyPalette.Cream
    )

    /** The cat on a shared [CatCanvas]; accessory layers use the exact same grid so they line up. */
    val cat: PixelSprite = PixelSprite.mirrored(
        listOf(
            "........", "........", "........", "........", // headroom for hats
            "........",
            ".oo.....",
            ".opoo...",
            ".opcoooo",
            ".occcccc",
            ".occcccc",
            ".occeccc",
            ".ocpcccp",
            "..occccc",
            "...ooooo",
            "..occccc",
            "..occcww",
            "..occcww",
            "..occcww",
            "..occooo",
            "........"
        ),
        catPalette
    )

    val yarnBall: PixelSprite = PixelSprite(
        listOf(
            "..oooo..",
            ".orlrro.",
            "orlrrrro",
            "orrlrrro",
            "orrrlrro",
            ".orrrlo.",
            "..oooo..",
            "......oo"
        ),
        mapOf(
            'o' to CozyPalette.WarmBrown,
            'r' to CozyPalette.DustyRose,
            'l' to CozyPalette.PalePink
        )
    )

    val plant: PixelSprite = PixelSprite(
        listOf(
            "..g..g..",
            ".ggg.ggg",
            ".gggggg.",
            "..gggg..",
            "...gg...",
            "..tttt..",
            "..tttt..",
            "...tt..."
        ),
        mapOf(
            'g' to Color(0xFF8DB580),
            't' to Color(0xFFC48B6B)
        )
    )

    private val gold = Color(0xFFF2C94C)

    val star: PixelSprite = PixelSprite(
        listOf(
            "...yy...",
            "...yy...",
            "yyyyyyyy",
            ".yyyyyy.",
            "..yyyy..",
            ".yyyyyy.",
            ".yy..yy.",
            "yy....yy"
        ),
        mapOf('y' to gold)
    )

    val gift: PixelSprite = PixelSprite(
        listOf(
            ".rr..rr.",
            "..rrrr..",
            "bbbrrbbb",
            "bbbrrbbb",
            "rrrrrrrr",
            "bbbrrbbb",
            "bbbrrbbb",
            "bbbrrbbb"
        ),
        mapOf('r' to CozyPalette.DustyRose, 'b' to Color(0xFFB8A6DB))
    )

    val heart: PixelSprite = PixelSprite(
        listOf(
            ".rr..rr.",
            "rrrrrrrr",
            "rrrrrrrr",
            "rrrrrrrr",
            ".rrrrrr.",
            "..rrrr..",
            "...rr...",
            "........"
        ),
        mapOf('r' to CozyPalette.DustyRose)
    )

    val trophy: PixelSprite = PixelSprite(
        listOf(
            "yyyyyyyy",
            "yyyyyyyy",
            ".yyyyyy.",
            "..yyyy..",
            "...yy...",
            "...yy...",
            "..yyyy..",
            ".yyyyyy."
        ),
        mapOf('y' to gold)
    )

    val stitch: PixelSprite = PixelSprite(
        listOf(
            "..oooo..",
            ".o....o.",
            "o..oo..o",
            "o.o..o.o",
            "o.o..o.o",
            "o..oo..o",
            ".o....o.",
            "..oooo.."
        ),
        mapOf('o' to CozyPalette.WarmBrown)
    )

    val flower: PixelSprite = PixelSprite(
        listOf(
            "..pppp..",
            ".ppyypp.",
            ".ppyypp.",
            "..pppp..",
            "...gg...",
            "..ggg...",
            "...gg.g.",
            "...gg.gg"
        ),
        mapOf(
            'p' to CozyPalette.DustyRose,
            'y' to gold,
            'g' to Color(0xFF8DB580)
        )
    )

    fun forIcon(icon: IconKey): PixelSprite = when (icon) {
        IconKey.YARN -> yarnBall
        IconKey.STAR -> star
        IconKey.GIFT -> gift
        IconKey.FLOWER -> flower
        IconKey.HEART -> heart
        IconKey.TROPHY -> trophy
        IconKey.STITCH -> stitch
    }
}

/**
 * Draws a [PixelSprite] with whole-pixel cells so edges stay crisp
 * (the Canvas equivalent of nearest-neighbor scaling).
 */
@Composable
fun PixelSpriteImage(
    sprite: PixelSprite,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    /** When set, every pixel is drawn in this color (used for locked-achievement silhouettes). */
    silhouette: Color? = null
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    Canvas(modifier = modifier.then(semanticsModifier)) {
        val cell = floor(minOf(size.width / sprite.width, size.height / sprite.height)).coerceAtLeast(1f)
        val offsetX = floor((size.width - cell * sprite.width) / 2f)
        val offsetY = floor((size.height - cell * sprite.height) / 2f)
        drawSprite(sprite, offsetX, offsetY, cell, silhouette)
    }
}

/**
 * Draws [sprite] with its top-left corner at ([left], [top]) using square cells of [cell] pixels.
 * Used by the sprite image, and by the room, which draws everything in one canvas so all the
 * pieces share exactly the same pixel grid.
 */
fun DrawScope.drawSprite(
    sprite: PixelSprite,
    left: Float,
    top: Float,
    cell: Float,
    silhouette: Color? = null
) {
    sprite.rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, char ->
            val color = sprite.colorAt(char)
            if (color != null) {
                drawRect(
                    color = silhouette ?: color,
                    topLeft = Offset(left + x * cell, top + y * cell),
                    size = Size(cell, cell)
                )
            }
        }
    }
}
