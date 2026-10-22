package com.crochetquest.app.domain.game

import java.time.Instant
import java.time.ZoneId

/** Injectable time source so day-based rules are deterministic in tests. */
interface GameClock {
    fun nowMillis(): Long
    val zone: ZoneId
}

object SystemGameClock : GameClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override val zone: ZoneId get() = ZoneId.systemDefault()
}

/** Local calendar day as "yyyy-MM-dd". */
fun GameClock.dayKeyFor(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toString()

fun GameClock.todayKey(): String = dayKeyFor(nowMillis())
