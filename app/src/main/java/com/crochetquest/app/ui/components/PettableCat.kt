package com.crochetquest.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class Heart(val id: Int, val dxDp: Int)

/**
 * The cat on the home screen. Tapping pets it: a few pixel hearts float up and [onPet] is
 * called (the screen plays a purr). Petting never gives rewards. [reactionTick] going up
 * (a session was logged, a project finished) makes the cat hop happily with a few hearts.
 * When [animate] is false hearts simply appear and fade out with no movement.
 */
@Composable
fun PettableCat(
    catName: String,
    accessoryKeys: List<String>,
    animate: Boolean,
    asleep: Boolean,
    reactionTick: Int,
    onPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hearts by remember { mutableStateOf(listOf<Heart>()) }
    var nextId by remember { mutableStateOf(0) }
    val hop = remember { Animatable(0f) }
    // Ignore the value we start with, so coming back to this screen doesn't replay old reactions.
    val firstTick = remember { reactionTick }

    fun spawnHeart() {
        val heart = Heart(nextId, Random.nextInt(-40, 41))
        nextId += 1
        hearts = (hearts + heart).takeLast(6)
    }

    LaunchedEffect(reactionTick) {
        if (reactionTick != firstTick) {
            if (animate) {
                repeat(2) {
                    hop.animateTo(1f, tween(130))
                    hop.animateTo(0f, tween(170))
                }
            }
            repeat(3) {
                spawnHeart()
                delay(170)
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedCat(
            catName = catName,
            accessoryKeys = accessoryKeys,
            animate = animate,
            asleep = asleep,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = -hop.value * 14.dp.toPx() }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Pet $catName",
                    role = Role.Button
                ) {
                    spawnHeart()
                    onPet()
                }
        )
        hearts.forEach { heart ->
            key(heart.id) {
                FloatingHeart(heart.dxDp, animate) {
                    hearts = hearts.filter { it.id != heart.id }
                }
            }
        }
    }
}

@Composable
private fun FloatingHeart(dxDp: Int, animate: Boolean, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (animate) progress.animateTo(1f, tween(900)) else delay(700)
        onDone()
    }
    PixelSpriteImage(
        sprite = Sprites.heart,
        modifier = Modifier
            .size(24.dp)
            .offset(x = dxDp.dp, y = (-28).dp)
            .graphicsLayer {
                translationY = -progress.value * 84.dp.toPx()
                alpha = if (animate) 1f - progress.value else 1f
            }
    )
}
