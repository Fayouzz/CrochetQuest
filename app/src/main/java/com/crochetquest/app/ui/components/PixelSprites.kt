package com.crochetquest.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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

object Sprites {
    private val catPalette = mapOf(
        'o' to CozyPalette.WarmBrown,
        'c' to Color(0xFFF6D3A8),
        'p' to CozyPalette.DustyRose,
        'e' to CozyPalette.DarkText,
        'w' to CozyPalette.Cream
    )

    val cat: PixelSprite = PixelSprite.mirrored(
        listOf(
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
}

/**
 * Draws a [PixelSprite] with whole-pixel cells so edges stay crisp
 * (the Canvas equivalent of nearest-neighbor scaling).
 */
@Composable
fun PixelSpriteImage(
    sprite: PixelSprite,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
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
        sprite.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, char ->
                val color = sprite.colorAt(char)
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(offsetX + x * cell, offsetY + y * cell),
                        size = Size(cell, cell)
                    )
                }
            }
        }
    }
}

/** The cat with an optional slow, gentle bob. The animation is skipped when [animate] is false. */
@Composable
fun BobbingCat(
    animate: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bob: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "catBob").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "catBobOffset"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    PixelSpriteImage(
        sprite = Sprites.cat,
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer { translationY = -bob.value * 5.dp.toPx() }
    )
}
