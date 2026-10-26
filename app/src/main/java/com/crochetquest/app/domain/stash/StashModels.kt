package com.crochetquest.app.domain.stash

import com.crochetquest.app.data.local.StashYarn

object StashLimits {
    const val MAX_NAME_LENGTH = 40
    const val MAX_AMOUNT = 999
}

/** A swatch to pick when adding a yarn color. The name makes the choice clear without relying on color alone. */
data class YarnSwatch(val name: String, val argb: Int)

object YarnSwatches {
    val all: List<YarnSwatch> = listOf(
        YarnSwatch("Cream", 0xFFF3E6D0.toInt()),
        YarnSwatch("Butter", 0xFFF2D98B.toInt()),
        YarnSwatch("Peach", 0xFFF6C3A0.toInt()),
        YarnSwatch("Coral", 0xFFE8836F.toInt()),
        YarnSwatch("Dusty rose", 0xFFD89BB5.toInt()),
        YarnSwatch("Berry", 0xFF9C4F6E.toInt()),
        YarnSwatch("Lavender", 0xFFB8A6DB.toInt()),
        YarnSwatch("Sky", 0xFF9CC5E3.toInt()),
        YarnSwatch("Teal", 0xFF5FA8A0.toInt()),
        YarnSwatch("Sage", 0xFF9DBA94.toInt()),
        YarnSwatch("Moss", 0xFF6B8E4E.toInt()),
        YarnSwatch("Cocoa", 0xFF73594B.toInt()),
        YarnSwatch("Charcoal", 0xFF4A4448.toInt())
    )

    fun nameFor(argb: Int): String? = all.firstOrNull { it.argb == argb }?.name
}

/** A stash color together with the projects it is linked to. */
data class StashItem(val yarn: StashYarn, val projectIds: List<Long>)
