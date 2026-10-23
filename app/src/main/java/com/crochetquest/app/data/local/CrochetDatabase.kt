package com.crochetquest.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlayerProfile::class,
        CrochetSession::class,
        StitchRecord::class,
        Project::class,
        ProjectUpdate::class,
        QuestCompletion::class,
        AchievementUnlock::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CrochetDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun stitchDao(): StitchDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectUpdateDao(): ProjectUpdateDao
    abstract fun questDao(): QuestDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        /** Stage 1 -> Stage 2: purely additive, so existing progress is kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_profile ADD COLUMN lifetimeYarnEarned INTEGER NOT NULL DEFAULT 0"
                )
                // Best available estimate for existing players: what they currently hold.
                db.execSQL("UPDATE player_profile SET lifetimeYarnEarned = yarnBalance")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `project` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`progressPercent` INTEGER NOT NULL, " +
                        "`targetDateEpochDay` INTEGER, " +
                        "`hookSize` TEXT NOT NULL, " +
                        "`materialNotes` TEXT NOT NULL, " +
                        "`stitchNotes` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`completedAt` INTEGER, " +
                        "`completionRewardGranted` INTEGER NOT NULL, " +
                        "`lastRewardedProgress` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `project_update` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`projectId` INTEGER NOT NULL, " +
                        "`loggedAt` INTEGER NOT NULL, " +
                        "`fromPercent` INTEGER NOT NULL, " +
                        "`toPercent` INTEGER NOT NULL, " +
                        "`rewarded` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_project_update_projectId` " +
                        "ON `project_update` (`projectId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quest_completion` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`questKey` TEXT NOT NULL, " +
                        "`periodKey` TEXT NOT NULL, " +
                        "`completedAt` INTEGER NOT NULL, " +
                        "`claimedAt` INTEGER, " +
                        "`associatedProjectId` INTEGER)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_quest_completion_questKey_periodKey` " +
                        "ON `quest_completion` (`questKey`, `periodKey`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `achievement` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`achievementKey` TEXT NOT NULL, " +
                        "`unlockedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_achievement_achievementKey` " +
                        "ON `achievement` (`achievementKey`)"
                )
            }
        }

        fun create(context: Context): CrochetDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CrochetDatabase::class.java,
                "crochet_quest.db"
            ).addMigrations(MIGRATION_1_2).build()
    }
}
