package com.crochetquest.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The single player profile (always id = 1). The level is NOT stored: it is derived from
 * [totalXp] by LevelCurve, so it can never drift out of sync.
 * [lifetimeYarnEarned] only ever goes up (spending does not reduce it); achievements use it.
 */
@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val catName: String,
    val totalXp: Int,
    val yarnBalance: Int,
    val createdAt: Long,
    val lifetimeYarnEarned: Int = 0
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

/**
 * A real-world crochet project. [status] and [category] hold enum names (see
 * domain/projects/ProjectModels.kt), so no Room type converters are needed.
 *
 * [completionRewardGranted] makes the completion reward one-time for the life of the project,
 * even if it is reopened and finished again. [lastRewardedProgress] is the high-water mark used
 * to decide whether a progress update is "meaningful", so sliding up and down cannot farm rewards.
 */
@Entity(tableName = "project")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    val status: String,
    val progressPercent: Int,
    val targetDateEpochDay: Long?,
    val hookSize: String,
    val materialNotes: String,
    val stitchNotes: String,
    val createdAt: Long,
    val completedAt: Long?,
    val completionRewardGranted: Boolean,
    val lastRewardedProgress: Int
)

/** History entry written whenever a project's progress moves forward. */
@Entity(tableName = "project_update", indices = [Index(value = ["projectId"])])
data class ProjectUpdate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val loggedAt: Long,
    val fromPercent: Int,
    val toPercent: Int,
    val rewarded: Boolean
)

/**
 * One instance of a quest. [periodKey] is "once" for one-time quests, a date for daily quests
 * or "W<monday-date>" for weekly ones. (questKey, periodKey) is unique, so a quest instance can
 * only ever be completed (and therefore claimed) once.
 * Completed = row exists. Claimed = [claimedAt] is set.
 */
@Entity(
    tableName = "quest_completion",
    indices = [Index(value = ["questKey", "periodKey"], unique = true)]
)
data class QuestCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questKey: String,
    val periodKey: String,
    val completedAt: Long,
    val claimedAt: Long?,
    val associatedProjectId: Long?
)

@Entity(
    tableName = "achievement",
    indices = [Index(value = ["achievementKey"], unique = true)]
)
data class AchievementUnlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val achievementKey: String,
    val unlockedAt: Long
)
