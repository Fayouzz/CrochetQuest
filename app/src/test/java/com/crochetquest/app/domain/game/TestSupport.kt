package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.StitchRecord
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TestClock(
    var millis: Long = Instant.parse("2026-01-01T12:00:00Z").toEpochMilli()
) : GameClock {
    override fun nowMillis(): Long = millis
    override val zone: ZoneId = ZoneOffset.UTC
    fun advanceDays(days: Long) {
        millis += days * 24 * 60 * 60 * 1000
    }
}

class FakeGameStore : GameStore {
    var profile: PlayerProfile? = null
    val sessions = mutableListOf<CrochetSession>()
    val stitches = mutableListOf<StitchRecord>()

    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
    override suspend fun getProfile(): PlayerProfile? = profile

    override suspend fun insertProfileIfAbsent(profile: PlayerProfile): Boolean {
        if (this.profile != null) return false
        this.profile = profile
        return true
    }

    override suspend fun updateProfile(profile: PlayerProfile) {
        this.profile = profile
    }

    override suspend fun countRewardedSessionsOnDay(dayKey: String): Int =
        sessions.count { it.dayKey == dayKey && it.rewarded }

    override suspend fun insertSession(session: CrochetSession) {
        sessions += session
    }

    override suspend fun insertStitchIfNew(record: StitchRecord): Boolean {
        if (stitches.any { it.stitchKey == record.stitchKey }) return false
        stitches += record
        return true
    }
}
