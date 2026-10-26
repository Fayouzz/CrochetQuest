package com.crochetquest.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observe(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun get(): PlayerProfile?

    /** Returns -1 when a profile already exists (insert ignored). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(profile: PlayerProfile): Long

    @Update
    suspend fun update(profile: PlayerProfile)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: CrochetSession): Long

    @Query("SELECT COUNT(*) FROM crochet_session WHERE dayKey = :dayKey AND rewarded = 1")
    suspend fun countRewardedOnDay(dayKey: String): Int

    @Query("SELECT COUNT(*) FROM crochet_session WHERE loggedAt >= :start AND loggedAt < :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM crochet_session")
    fun observeTotal(): Flow<Int>

    @Query("SELECT * FROM crochet_session ORDER BY id")
    suspend fun getAll(): List<CrochetSession>
}

@Dao
interface StitchDao {
    /** Returns -1 when the stitch was already recorded (unique index, insert ignored). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(record: StitchRecord): Long

    @Query("SELECT COUNT(*) FROM stitch_record")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM stitch_record WHERE firstRecordedAt >= :start AND firstRecordedAt < :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM stitch_record")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM stitch_record ORDER BY id")
    suspend fun getAll(): List<StitchRecord>
}

@Dao
interface ProjectDao {
    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Query("SELECT * FROM project WHERE id = :id")
    suspend fun get(id: Long): Project?

    @Query("SELECT * FROM project ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM project ORDER BY id")
    suspend fun getAll(): List<Project>

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM project")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM project WHERE completionRewardGranted = 1")
    suspend fun countFinished(): Int

    @Query("SELECT COUNT(*) FROM project WHERE completionRewardGranted = 1 AND category = :category")
    suspend fun countFinishedInCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM project WHERE materialNotes != ''")
    suspend fun countWithMaterialNotes(): Int
}

@Dao
interface ProjectUpdateDao {
    @Insert
    suspend fun insert(update: ProjectUpdate)

    @Query("DELETE FROM project_update WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)

    @Query("SELECT COUNT(*) FROM project_update WHERE loggedAt >= :start AND loggedAt < :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Query("SELECT * FROM project_update WHERE projectId = :projectId ORDER BY loggedAt DESC, id DESC")
    fun observeForProject(projectId: Long): Flow<List<ProjectUpdate>>

    @Query("SELECT COUNT(*) FROM project_update")
    fun observeTotal(): Flow<Int>

    @Query("SELECT * FROM project_update ORDER BY id")
    suspend fun getAll(): List<ProjectUpdate>
}

@Dao
interface QuestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(completion: QuestCompletion): Long

    @Query("SELECT * FROM quest_completion WHERE questKey = :questKey AND periodKey = :periodKey")
    suspend fun get(questKey: String, periodKey: String): QuestCompletion?

    @Query("SELECT * FROM quest_completion")
    suspend fun getAll(): List<QuestCompletion>

    @Query("SELECT COUNT(*) FROM quest_completion")
    suspend fun count(): Int

    /** Only flips an unclaimed row; returns how many rows changed (0 or 1). */
    @Query(
        "UPDATE quest_completion SET claimedAt = :claimedAt " +
            "WHERE questKey = :questKey AND periodKey = :periodKey AND claimedAt IS NULL"
    )
    suspend fun markClaimed(questKey: String, periodKey: String, claimedAt: Long): Int

    @Query("SELECT * FROM quest_completion")
    fun observeAll(): Flow<List<QuestCompletion>>
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(unlock: AchievementUnlock): Long

    @Query("SELECT * FROM achievement")
    suspend fun getAll(): List<AchievementUnlock>

    @Query("SELECT * FROM achievement")
    fun observeAll(): Flow<List<AchievementUnlock>>
}

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(item: InventoryItem): Long

    @Query("SELECT * FROM inventory_item")
    suspend fun getAll(): List<InventoryItem>

    @Query("SELECT * FROM inventory_item")
    fun observeAll(): Flow<List<InventoryItem>>
}

@Dao
interface EquippedDao {
    /** Upsert: wearing something in a slot replaces whatever was there. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(equipped: EquippedCosmetic)

    @Query("DELETE FROM equipped_cosmetic WHERE accessorySlot = :slot")
    suspend fun clear(slot: String)

    @Query("DELETE FROM equipped_cosmetic")
    suspend fun clearAll()

    @Query("SELECT * FROM equipped_cosmetic")
    suspend fun getAll(): List<EquippedCosmetic>

    @Query("SELECT * FROM equipped_cosmetic")
    fun observeAll(): Flow<List<EquippedCosmetic>>
}

/** Used only by "Reset game progress". Always called inside one transaction. */
@Dao
interface ResetDao {
    @Query("DELETE FROM player_profile")
    suspend fun clearProfile()

    @Query("DELETE FROM crochet_session")
    suspend fun clearSessions()

    @Query("DELETE FROM stitch_record")
    suspend fun clearStitches()

    @Query("DELETE FROM project")
    suspend fun clearProjects()

    @Query("DELETE FROM project_update")
    suspend fun clearProjectUpdates()

    @Query("DELETE FROM quest_completion")
    suspend fun clearQuestCompletions()

    @Query("DELETE FROM achievement")
    suspend fun clearAchievements()

    @Query("DELETE FROM inventory_item")
    suspend fun clearInventory()

    @Query("DELETE FROM equipped_cosmetic")
    suspend fun clearEquipped()

    @Query("DELETE FROM stash_yarn")
    suspend fun clearStash()

    @Query("DELETE FROM project_yarn_link")
    suspend fun clearStashLinks()
}

@Dao
interface StashDao {
    @Insert
    suspend fun insert(yarn: StashYarn): Long

    @Update
    suspend fun update(yarn: StashYarn)

    @Query("SELECT * FROM stash_yarn WHERE id = :id")
    suspend fun get(id: Long): StashYarn?

    @Query("DELETE FROM stash_yarn WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM stash_yarn ORDER BY id")
    suspend fun getAll(): List<StashYarn>

    @Query("SELECT * FROM stash_yarn")
    fun observeAll(): Flow<List<StashYarn>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: ProjectYarnLink): Long

    @Query("DELETE FROM project_yarn_link WHERE projectId = :projectId AND yarnId = :yarnId")
    suspend fun deleteLink(projectId: Long, yarnId: Long)

    @Query("DELETE FROM project_yarn_link WHERE yarnId = :yarnId")
    suspend fun deleteLinksForYarn(yarnId: Long)

    @Query("DELETE FROM project_yarn_link WHERE projectId = :projectId")
    suspend fun deleteLinksForProject(projectId: Long)

    @Query("SELECT * FROM project_yarn_link")
    suspend fun getAllLinks(): List<ProjectYarnLink>

    @Query("SELECT * FROM project_yarn_link")
    fun observeLinks(): Flow<List<ProjectYarnLink>>

    @Query(
        "SELECT COUNT(*) FROM (SELECT projectId FROM project_yarn_link " +
            "GROUP BY projectId HAVING COUNT(*) >= :min)"
    )
    suspend fun countProjectsWithAtLeast(min: Int): Int
}

/** Bulk inserts used only by "Restore from backup" (after everything has been cleared). */
@Dao
interface BackupDao {
    @Insert suspend fun insertProfile(items: List<PlayerProfile>)
    @Insert suspend fun insertSessions(items: List<CrochetSession>)
    @Insert suspend fun insertStitches(items: List<StitchRecord>)
    @Insert suspend fun insertProjects(items: List<Project>)
    @Insert suspend fun insertProjectUpdates(items: List<ProjectUpdate>)
    @Insert suspend fun insertQuestCompletions(items: List<QuestCompletion>)
    @Insert suspend fun insertAchievements(items: List<AchievementUnlock>)
    @Insert suspend fun insertInventory(items: List<InventoryItem>)
    @Insert suspend fun insertEquipped(items: List<EquippedCosmetic>)
    @Insert suspend fun insertStash(items: List<StashYarn>)
    @Insert suspend fun insertLinks(items: List<ProjectYarnLink>)
}
