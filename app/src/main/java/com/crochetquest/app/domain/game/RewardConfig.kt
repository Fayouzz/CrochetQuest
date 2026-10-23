package com.crochetquest.app.domain.game

/**
 * All tunable reward numbers live here (never duplicated in UI code).
 */
object RewardConfig {
    data class Reward(val xp: Int, val yarn: Int)

    val LOG_SESSION = Reward(xp = 10, yarn = 5)
    val NEW_STITCH = Reward(xp = 15, yarn = 10)
    val PROJECT_UPDATE = Reward(xp = 5, yarn = 2)
    val PROJECT_COMPLETE = Reward(xp = 30, yarn = 20)
    val WEEKLY_QUEST = Reward(xp = 20, yarn = 10)
    /** Reward of the "Make a handmade gift" milestone quest (granted when that quest is claimed). */
    val HANDMADE_GIFT_BONUS = Reward(xp = 10, yarn = 10)

    /** Anti-farming: only this many sessions per local day earn rewards. Extra ones are still saved. */
    const val MAX_REWARDED_SESSIONS_PER_DAY = 3

    /** Starting yarn: two sessions later the player can afford the first 20-yarn accessory. */
    const val STARTING_YARN = 10

    /**
     * A progress update only earns a reward when it moves this many percentage points past the
     * project's best previously-rewarded progress, so sliding back and forth cannot farm yarn.
     */
    const val MEANINGFUL_PROGRESS_STEP = 10

    const val DEFAULT_CAT_NAME = "Mochi"
    const val MAX_PROJECT_NAME_LENGTH = 60
    const val MAX_PROJECT_NOTE_LENGTH = 1000
    const val MAX_HOOK_SIZE_LENGTH = 20
    const val MAX_CAT_NAME_LENGTH = 20
    const val MAX_STITCH_NAME_LENGTH = 40
}
