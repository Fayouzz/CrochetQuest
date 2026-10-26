package com.crochetquest.app.domain.time

import java.time.Instant
import java.time.ZoneId

/**
 * Local day/night rules. The cat naps and the room's window shows the moon between
 * [NIGHT_START_HOUR] (inclusive) and [NIGHT_END_HOUR] (exclusive).
 */
object DayCycle {
    const val NIGHT_START_HOUR = 20
    const val NIGHT_END_HOUR = 7

    fun isNight(millis: Long, zone: ZoneId): Boolean {
        val hour = Instant.ofEpochMilli(millis).atZone(zone).hour
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
    }

    /**
     * The next moment after [millis] when the calendar day or the day/night phase changes
     * (local 00:00, 07:00 or 20:00). The UI sleeps until then, so there is no polling.
     */
    fun nextBoundary(millis: Long, zone: ZoneId): Long {
        val today = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return listOf(
            today.atStartOfDay(zone),
            today.atTime(NIGHT_END_HOUR, 0).atZone(zone),
            today.atTime(NIGHT_START_HOUR, 0).atZone(zone),
            today.plusDays(1).atStartOfDay(zone)
        ).map { it.toInstant().toEpochMilli() }.filter { it > millis }.min()
    }
}

/** Minutes for a timed session: rounded to the nearest minute, at least 1, at most 24 hours. */
fun minutesForTimer(startMillis: Long, nowMillis: Long): Int =
    (((nowMillis - startMillis).coerceAtLeast(0L) + 30_000L) / 60_000L).toInt().coerceIn(1, 1440)
