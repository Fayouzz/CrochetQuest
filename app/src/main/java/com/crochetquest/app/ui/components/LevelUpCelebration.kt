package com.crochetquest.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.crochetquest.app.ui.LevelUpEvent
import kotlinx.coroutines.delay

/**
 * A brief, lightweight "level up" card: one small spring-in, no particles, auto-dismissing
 * after a couple of seconds (or on tap). With [animate] false it simply appears and disappears.
 */
@Composable
fun LevelUpCelebration(
    event: LevelUpEvent,
    animate: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember(event) { Animatable(if (animate) 0.6f else 1f) }
    LaunchedEffect(event, animate) {
        if (animate) {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    LaunchedEffect(event) {
        delay(2_600L)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(onClickLabel = "Dismiss", onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        CozyCard(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelSpriteImage(Sprites.star, Modifier.size(28.dp))
                PixelSpriteImage(Sprites.trophy, Modifier.size(28.dp))
                PixelSpriteImage(Sprites.star, Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Level ${event.level}!", style = MaterialTheme.typography.headlineMedium)
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
