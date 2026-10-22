package com.crochetquest.app.domain.game

/**
 * All tunable reward numbers live here (never duplicated in UI code).
 * Entries marked "later" are defined now but wired up in later stages.
 */
object RewardConfig {
    data class Reward(val xp: Int, val yarn: Int)

    val LOG_SESSION = Reward(xp = 10, yarn = 5)
    val NEW_STITCH = Reward(xp = 15, yarn = 10)
    val PROJECT_UPDATE = Reward(xp = 5, yarn = 2)      // later
    val PROJECT_COMPLETE = Reward(xp = 30, yarn = 20)  // later
    val WEEKLY_QUEST = Reward(xp = 20, yarn = 10)      // later
    val HANDMADE_GIFT_BONUS = Reward(xp = 10, yarn = 10) // later

    /** Anti-farming: only this many sessions per local day earn rewards. Extra ones are still saved. */
    const val MAX_REWARDED_SESSIONS_PER_DAY = 3

    /** Starting yarn: two sessions later the player can afford the first 20-yarn accessory. */
    const val STARTING_YARN = 10

    const val DEFAULT_CAT_NAME = "Mochi"
    const val MAX_CAT_NAME_LENGTH = 20
    const val MAX_STITCH_NAME_LENGTH = 40
}
