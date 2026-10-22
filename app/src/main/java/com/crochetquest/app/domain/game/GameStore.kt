package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.StitchRecord

/**
 * Persistence boundary used by [GameService]. Room implements it in production;
 * tests use an in-memory fake so game rules run as fast plain-JVM unit tests.
 */
interface GameStore {
    /** Runs [block] atomically (a Room transaction in production). */
    suspend fun <T> inTransaction(block: suspend () -> T): T

    suspend fun getProfile(): PlayerProfile?
    suspend fun insertProfileIfAbsent(profile: PlayerProfile): Boolean
    suspend fun updateProfile(profile: PlayerProfile)

    suspend fun countRewardedSessionsOnDay(dayKey: String): Int
    suspend fun insertSession(session: CrochetSession)

    /** True if the stitch was new and got recorded; false if it already existed. */
    suspend fun insertStitchIfNew(record: StitchRecord): Boolean
}
