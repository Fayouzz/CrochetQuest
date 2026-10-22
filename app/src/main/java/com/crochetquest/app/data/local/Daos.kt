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

    @Query("SELECT COUNT(*) FROM crochet_session")
    fun observeTotal(): Flow<Int>
}

@Dao
interface StitchDao {
    /** Returns -1 when the stitch was already recorded (unique index, insert ignored). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(record: StitchRecord): Long

    @Query("SELECT COUNT(*) FROM stitch_record")
    fun observeCount(): Flow<Int>
}
