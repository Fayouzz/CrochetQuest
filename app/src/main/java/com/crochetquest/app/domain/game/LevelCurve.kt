package com.crochetquest.app.domain.game

data class LevelInfo(
    val level: Int,
    val title: String,
    val totalXp: Int,
    /** XP earned since reaching [level]. */
    val xpIntoLevel: Int,
    /** XP span between [level] and the next level. */
    val xpNeededForNext: Int
) {
    val progress: Float
        get() = if (xpNeededForNext <= 0) 0f else (xpIntoLevel.toFloat() / xpNeededForNext).coerceIn(0f, 1f)
}

/**
 * Transparent level curve: L1=0, L2=100, L3=250, L4=450, L5=700, L6=1000, ...
 * Closed form: xpRequired(level) = 25 * (level - 1) * (level + 2)
 * (each level needs 50 more XP than the previous gap). Levels are always derived from total XP.
 */
object LevelCurve {
    private val titles = listOf(
        "Tiny Beginner", "Yarn Curious", "Little Stitcher",
        "Cozy Creator", "Stitch Explorer", "Master Maker"
    )

    fun xpRequiredForLevel(level: Int): Long {
        require(level >= 1) { "level must be >= 1" }
        return 25L * (level - 1) * (level + 2)
    }

    fun levelForXp(totalXp: Int): Int {
        val xp = totalXp.coerceAtLeast(0).toLong()
        var level = 1
        while (xpRequiredForLevel(level + 1) <= xp) level++
        return level
    }

    fun titleForLevel(level: Int): String =
        titles[(level - 1).coerceIn(0, titles.lastIndex)]

    fun infoFor(totalXp: Int): LevelInfo {
        val xp = totalXp.coerceAtLeast(0)
        val level = levelForXp(xp)
        val start = xpRequiredForLevel(level)
        val next = xpRequiredForLevel(level + 1)
        return LevelInfo(
            level = level,
            title = titleForLevel(level),
            totalXp = xp,
            xpIntoLevel = (xp - start).toInt(),
            xpNeededForNext = (next - start).toInt()
        )
    }
}
