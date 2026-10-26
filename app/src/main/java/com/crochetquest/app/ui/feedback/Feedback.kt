package com.crochetquest.app.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.crochetquest.app.CrochetQuestApp
import com.crochetquest.app.R

/** The three little sounds in the app (each comes with a matching soft haptic). */
enum class FeedbackCue { CLICK, CHIME, PURR }

/**
 * Plays the bundled sound effects with SoundPool (short, low-latency, no permissions).
 * The sounds are tiny WAV files in res/raw.
 */
class SoundPlayer(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<FeedbackCue, Int> = mapOf(
        FeedbackCue.CLICK to pool.load(context, R.raw.sfx_click, 1),
        FeedbackCue.CHIME to pool.load(context, R.raw.sfx_chime, 1),
        FeedbackCue.PURR to pool.load(context, R.raw.sfx_purr, 1)
    )

    fun play(cue: FeedbackCue, volume: Float = 1f) {
        val id = ids[cue] ?: return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()
}

/**
 * Returns a function that plays a cue's sound and haptic, but only while the player has
 * "Sounds & haptics" switched on. Purring is rate-limited so rapid petting doesn't stack sounds.
 */
@Composable
fun rememberFeedback(enabled: Boolean): (FeedbackCue) -> Unit {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val player = remember { (context.applicationContext as CrochetQuestApp).container.soundPlayer }
    val enabledNow = rememberUpdatedState(enabled)
    val lastPurr = remember { longArrayOf(0L) }

    return remember(player, haptics) {
        val play: (FeedbackCue) -> Unit = { cue ->
            if (enabledNow.value) {
                when (cue) {
                    FeedbackCue.CLICK -> {
                        player.play(FeedbackCue.CLICK, 0.7f)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    FeedbackCue.CHIME -> {
                        player.play(FeedbackCue.CHIME, 0.8f)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    FeedbackCue.PURR -> {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastPurr[0] > 1_300L) {
                            lastPurr[0] = now
                            player.play(FeedbackCue.PURR, 0.9f)
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
        }
        play
    }
}
