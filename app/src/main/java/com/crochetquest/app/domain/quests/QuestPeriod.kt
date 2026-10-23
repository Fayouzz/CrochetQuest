package com.crochetquest.app.domain.quests

import com.crochetquest.app.domain.game.GameClock
import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.TemporalAdjusters

/** A time window for one quest instance: [startMillis] inclusive, [endMillis] exclusive. */
data class QuestPeriod(val key: String, val startMillis: Long, val endMillis: Long)

/** Daily periods are local calendar days; weekly periods run Monday to Sunday. */
fun GameClock.periodFor(policy: RepeatPolicy, atMillis: Long = nowMillis()): QuestPeriod {
    val date = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate()
    return when (policy) {
        RepeatPolicy.ONCE -> QuestPeriod("once", 0L, Long.MAX_VALUE)
        RepeatPolicy.DAILY -> QuestPeriod(
            key = date.toString(),
            startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
        RepeatPolicy.WEEKLY -> {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            QuestPeriod(
                key = "W$monday",
                startMillis = monday.atStartOfDay(zone).toInstant().toEpochMilli(),
                endMillis = monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        }
    }
}
