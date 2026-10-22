package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.PlayerProfile

/** Pure helpers for XP / yarn math. They never produce a negative balance. */
fun PlayerProfile.withReward(xp: Int, yarn: Int): PlayerProfile {
    require(xp >= 0 && yarn >= 0) { "rewards cannot be negative" }
    return copy(totalXp = saturatingAdd(totalXp, xp), yarnBalance = saturatingAdd(yarnBalance, yarn))
}

/** Returns the updated profile, or null when the cost is invalid or unaffordable. */
fun PlayerProfile.trySpendYarn(cost: Int): PlayerProfile? =
    if (cost <= 0 || cost > yarnBalance) null else copy(yarnBalance = yarnBalance - cost)

private fun saturatingAdd(a: Int, b: Int): Int =
    (a.toLong() + b.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
