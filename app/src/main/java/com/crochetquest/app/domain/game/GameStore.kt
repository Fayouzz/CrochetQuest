package com.crochetquest.app.domain.game

import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.EquippedCosmetic
import com.crochetquest.app.data.local.InventoryItem
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.ProjectYarnLink
import com.crochetquest.app.data.local.StashYarn
import com.crochetquest.app.domain.backup.BackupSnapshot
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.data.local.QuestCompletion
import com.crochetquest.app.data.local.StitchRecord

/**
 * Persistence boundary used by [GameService]. Room implements it in production;
 * tests use an in-memory fake so game rules run as fast plain-JVM unit tests.
 */
interface GameStore {
    /** Runs [block] atomically (a Room transaction in production). */
    suspend fun <T> inTransaction(block: suspend () -> T): T

    // ---- profile ----
    suspend fun getProfile(): PlayerProfile?
    suspend fun insertProfileIfAbsent(profile: PlayerProfile): Boolean
    suspend fun updateProfile(profile: PlayerProfile)

    // ---- sessions and stitches ----
    suspend fun countRewardedSessionsOnDay(dayKey: String): Int
    suspend fun insertSession(session: CrochetSession)
    suspend fun countSessionsBetween(startMillis: Long, endMillis: Long): Int

    /** True if the stitch was new and got recorded; false if it already existed. */
    suspend fun insertStitchIfNew(record: StitchRecord): Boolean
    suspend fun countStitches(): Int
    suspend fun countStitchesBetween(startMillis: Long, endMillis: Long): Int

    // ---- projects ----
    suspend fun insertProject(project: Project): Long
    suspend fun updateProject(project: Project)
    suspend fun getProject(id: Long): Project?
    suspend fun deleteProject(id: Long)
    suspend fun countProjects(): Int
    /** Projects that have been finished at least once (completion reward granted). */
    suspend fun countFinishedProjects(): Int
    suspend fun countFinishedProjectsInCategory(category: String): Int
    suspend fun countProjectsWithMaterialNotes(): Int
    suspend fun insertProjectUpdate(update: ProjectUpdate)
    suspend fun countProjectUpdatesBetween(startMillis: Long, endMillis: Long): Int

    // ---- quests ----
    suspend fun getQuestCompletion(questKey: String, periodKey: String): QuestCompletion?
    suspend fun allQuestCompletions(): List<QuestCompletion>
    suspend fun countQuestCompletions(): Int

    /** True if the completion was new. The (quest, period) pair is unique. */
    suspend fun insertQuestCompletionIfAbsent(completion: QuestCompletion): Boolean

    /** True only for the single call that flips an unclaimed completion to claimed. */
    suspend fun markQuestClaimed(questKey: String, periodKey: String, claimedAt: Long): Boolean

    // ---- achievements ----
    suspend fun allAchievementUnlocks(): List<AchievementUnlock>
    suspend fun insertAchievementIfAbsent(unlock: AchievementUnlock): Boolean

    // ---- wardrobe ----
    suspend fun allInventory(): List<InventoryItem>

    /** True if the item was new. The unique index makes double purchases impossible. */
    suspend fun insertInventoryItemIfAbsent(item: InventoryItem): Boolean
    suspend fun allEquipped(): List<EquippedCosmetic>
    suspend fun setEquipped(slot: String, itemKey: String)
    suspend fun clearEquipped(slot: String)
    suspend fun clearAllEquipped()

    // ---- reset ----
    /** Deletes all saved game data (used by "Reset game progress"). */
    suspend fun resetAllData()

    // ---- stats ----
    suspend fun allSessions(): List<CrochetSession>
    suspend fun allStitches(): List<StitchRecord>
    suspend fun allProjects(): List<Project>

    // ---- yarn stash ----
    suspend fun insertStashYarn(yarn: StashYarn): Long
    suspend fun updateStashYarn(yarn: StashYarn)
    suspend fun getStashYarn(id: Long): StashYarn?
    /** Also removes every link to this color. */
    suspend fun deleteStashYarn(id: Long)
    suspend fun allStashYarn(): List<StashYarn>
    suspend fun allProjectYarnLinks(): List<ProjectYarnLink>
    suspend fun insertProjectYarnLink(link: ProjectYarnLink)
    suspend fun deleteProjectYarnLink(projectId: Long, yarnId: Long)
    /** Number of projects that have at least [min] stash colors linked. */
    suspend fun countProjectsWithColors(min: Int): Int

    // ---- backup and restore ----
    suspend fun snapshot(): BackupSnapshot

    /** Replaces ALL saved data with [snapshot]. Always call inside one transaction. */
    suspend fun replaceAll(snapshot: BackupSnapshot)
}
