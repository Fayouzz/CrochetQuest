package com.crochetquest.app.data.repository

import androidx.room.withTransaction
import com.crochetquest.app.data.local.CrochetDatabase
import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.StitchRecord
import com.crochetquest.app.domain.game.GameStore
import kotlinx.coroutines.flow.Flow

/** Room-backed implementation of [GameStore], plus observable flows for the UI. */
class GameRepository(private val db: CrochetDatabase) : GameStore {
    private val profileDao = db.profileDao()
    private val sessionDao = db.sessionDao()
    private val stitchDao = db.stitchDao()

    val profile: Flow<PlayerProfile?> = profileDao.observe()
    val sessionCount: Flow<Int> = sessionDao.observeTotal()
    val stitchCount: Flow<Int> = stitchDao.observeCount()

    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        db.withTransaction { block() }

    override suspend fun getProfile(): PlayerProfile? = profileDao.get()

    override suspend fun insertProfileIfAbsent(profile: PlayerProfile): Boolean =
        profileDao.insertIfAbsent(profile) != -1L

    override suspend fun updateProfile(profile: PlayerProfile) = profileDao.update(profile)

    override suspend fun countRewardedSessionsOnDay(dayKey: String): Int =
        sessionDao.countRewardedOnDay(dayKey)

    override suspend fun insertSession(session: CrochetSession) {
        sessionDao.insert(session)
    }

    override suspend fun insertStitchIfNew(record: StitchRecord): Boolean =
        stitchDao.insertIfNew(record) != -1L
}
