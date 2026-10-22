package com.crochetquest.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The single player profile (always id = 1). The level is NOT stored: it is derived from
 * [totalXp] by LevelCurve, so it can never drift out of sync.
 */
@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val catName: String,
    val totalXp: Int,
    val yarnBalance: Int,
    val createdAt: Long
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/** One logged crochet session. [rewarded] is false once the daily reward limit was reached. */
@Entity(tableName = "crochet_session")
data class CrochetSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loggedAt: Long,
    val dayKey: String,
    val durationMinutes: Int?,
    val rewarded: Boolean
)

/** A stitch the player has recorded. [stitchKey] is the normalized name and is unique. */
@Entity(
    tableName = "stitch_record",
    indices = [Index(value = ["stitchKey"], unique = true)]
)
data class StitchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stitchKey: String,
    val stitchName: String,
    val firstRecordedAt: Long
)
