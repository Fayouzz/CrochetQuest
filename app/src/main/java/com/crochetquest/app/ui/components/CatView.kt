package com.crochetquest.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The cat plus whatever it is wearing, stacked as layers on one shared pixel grid.
 * [accessoryKeys] may hold at most one item per slot; layers are drawn body to head.
 * Items without art are skipped, so the cat always renders.
 */
@Composable
fun LayeredCat(
    catName: String,
    accessoryKeys: List<String>,
    modifier: Modifier = Modifier,
    blinking: Boolean = false
) {
    val worn = remember(accessoryKeys) {
        accessoryKeys
            .mapNotNull { key -> CosmeticCatalog.find(key) }
            .filter { CosmeticSprites.forItem(it.key) != null }
            .sortedBy { it.slot.ordinal }
    }
    val description = if (worn.isEmpty()) {
        "$catName, your pixel-art cat"
    } else {
        "$catName, your pixel-art cat, wearing ${worn.joinToString(", ") { it.name.lowercase() }}"
    }
    Box(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        PixelSpriteImage(Sprites.cat, Modifier.fillMaxSize())
        if (blinking) PixelSpriteImage(CosmeticSprites.blink, Modifier.fillMaxSize())
        worn.forEach { item ->
            CosmeticSprites.forItem(item.key)?.let { PixelSpriteImage(it, Modifier.fillMaxSize()) }
        }
    }
}

/**
 * [LayeredCat] with a slow idle bob and an occasional blink. Both stop completely when
 * [animate] is false (the in-app toggle or Android's remove-animations setting).
 */
@Composable
fun AnimatedCat(
    catName: String,
    accessoryKeys: List<String>,
    animate: Boolean,
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

    var blinking by remember { mutableStateOf(false) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(Random.nextLong(2_500L, 5_500L))
                blinking = true
                delay(150L)
                blinking = false
            }
        }
    }

    LayeredCat(
        catName = catName,
        accessoryKeys = accessoryKeys,
        blinking = blinking && animate,
        modifier = modifier.graphicsLayer { translationY = -bob.value * 5.dp.toPx() }
    )
}
