package com.veyra.app.data.workout

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkoutEntity::class, WorkoutSetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class VeyraDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var instance: VeyraDatabase? = null

        fun get(context: Context): VeyraDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VeyraDatabase::class.java,
                "veyra.db",
            ).build().also { instance = it }
        }
    }
}
