package com.batb4016.jarpick.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [JarEntity::class, OptionEntity::class, PickHistoryEntity::class, RoundStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class JarPickDatabase : RoomDatabase() {
    abstract fun dao(): JarPickDao

    companion object {
        @Volatile private var instance: JarPickDatabase? = null

        fun get(context: Context): JarPickDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JarPickDatabase::class.java,
                    "jarpick.db",
                ).build().also { instance = it }
            }
        }
    }
}
