package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.StitchRecord

data class SessionResult(
    val xpEarned: Int,
    val yarnEarned: Int,
    /** False when the daily reward limit was already reached (the session is still saved). */
    val sessionRewarded: Boolean,
    /** Set when the player recorded a stitch for the first time. */
    val newStitchName: String?,
    /** Set when the player typed a stitch they had already recorded (no reward). */
    val knownStitchName: String?,
    val levelBefore: Int,
    val levelAfter: Int
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

/**
 * Single source of truth for XP, yarn and level changes. UI code never touches reward math.
 * Every mutating operation runs inside one transaction.
 */
class GameService(
    private val store: GameStore,
    private val clock: GameClock = SystemGameClock
) {
    /** Idempotent: if a profile already exists it is returned unchanged. */
    suspend fun createProfile(catName: String): PlayerProfile = store.inTransaction {
        store.getProfile() ?: run {
            val profile = PlayerProfile(
                catName = cleanCatName(catName) ?: RewardConfig.DEFAULT_CAT_NAME,
                totalXp = 0,
                yarnBalance = RewardConfig.STARTING_YARN,
                createdAt = clock.nowMillis()
            )
            store.insertProfileIfAbsent(profile)
            profile
        }
    }

    /** Returns false if there is no profile or the name is blank. */
    suspend fun renameCat(newName: String): Boolean = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction false
        val cleaned = cleanCatName(newName) ?: return@inTransaction false
        store.updateProfile(profile.copy(catName = cleaned))
        true
    }

    suspend fun rewardedSessionsToday(): Int = store.countRewardedSessionsOnDay(clock.todayKey())

    /**
     * Logs a crochet session. Rules:
     *  - the first MAX_REWARDED_SESSIONS_PER_DAY sessions each local day earn the session reward;
     *    later ones are still recorded but earn nothing (no pressure, no farming);
     *  - an optional stitch name earns the new-stitch reward only the first time that stitch
     *    (case/whitespace-insensitive) is ever recorded.
     * Returns null when there is no profile yet.
     */
    suspend fun logSession(
        durationMinutes: Int? = null,
        newStitchName: String? = null
    ): SessionResult? = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction null
        val now = clock.nowMillis()
        val dayKey = clock.dayKeyFor(now)

        var xp = 0
        var yarn = 0

        val sessionRewarded =
            store.countRewardedSessionsOnDay(dayKey) < RewardConfig.MAX_REWARDED_SESSIONS_PER_DAY
        if (sessionRewarded) {
            xp += RewardConfig.LOG_SESSION.xp
            yarn += RewardConfig.LOG_SESSION.yarn
        }
        store.insertSession(
            CrochetSession(
                loggedAt = now,
                dayKey = dayKey,
                durationMinutes = durationMinutes?.takeIf { it in 1..1440 },
                rewarded = sessionRewarded
            )
        )

        var rewardedStitch: String? = null
        var knownStitch: String? = null
        val stitchName = cleanStitchName(newStitchName)
        if (stitchName != null) {
            val isNew = store.insertStitchIfNew(
                StitchRecord(
                    stitchKey = stitchName.lowercase(),
                    stitchName = stitchName,
                    firstRecordedAt = now
                )
            )
            if (isNew) {
                rewardedStitch = stitchName
                xp += RewardConfig.NEW_STITCH.xp
                yarn += RewardConfig.NEW_STITCH.yarn
            } else {
                knownStitch = stitchName
            }
        }

        val updated = profile.withReward(xp, yarn)
        if (updated != profile) store.updateProfile(updated)

        SessionResult(
            xpEarned = xp,
            yarnEarned = yarn,
            sessionRewarded = sessionRewarded,
            newStitchName = rewardedStitch,
            knownStitchName = knownStitch,
            levelBefore = LevelCurve.levelForXp(profile.totalXp),
            levelAfter = LevelCurve.levelForXp(updated.totalXp)
        )
    }

    /** Deducts yarn if affordable. Used by the shop in a later stage. Never goes negative. */
    suspend fun spendYarn(cost: Int): Boolean = store.inTransaction {
        val profile = store.getProfile() ?: return@inTransaction false
        val updated = profile.trySpendYarn(cost) ?: return@inTransaction false
        store.updateProfile(updated)
        true
    }

    private fun cleanCatName(raw: String): String? =
        raw.trim().replace(WHITESPACE, " ").take(RewardConfig.MAX_CAT_NAME_LENGTH).trim()
            .ifEmpty { null }

    private fun cleanStitchName(raw: String?): String? =
        raw?.trim()?.replace(WHITESPACE, " ")?.take(RewardConfig.MAX_STITCH_NAME_LENGTH)?.trim()
            ?.ifEmpty { null }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
