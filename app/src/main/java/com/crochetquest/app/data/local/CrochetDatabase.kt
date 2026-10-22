package com.crochetquest.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlayerProfile::class, CrochetSession::class, StitchRecord::class],
    version = 1,
    exportSchema = false
)
abstract class CrochetDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun stitchDao(): StitchDao

    companion object {
        fun create(context: Context): CrochetDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CrochetDatabase::class.java,
                "crochet_quest.db"
            ).build()
    }
}
