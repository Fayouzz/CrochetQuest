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
