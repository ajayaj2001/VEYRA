package com.veyra.app.data.workout

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
    ],
    version = 3,
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN healthConnectId TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN exerciseNote TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_exercises (
                        routineId INTEGER NOT NULL,
                        exerciseKey TEXT NOT NULL,
                        exerciseName TEXT NOT NULL,
                        equipment TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        targetSets INTEGER NOT NULL,
                        defaultWeightKg TEXT NOT NULL,
                        defaultReps TEXT NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        PRIMARY KEY(routineId, exerciseKey)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_routineId ON routine_exercises(routineId)")
            }
        }
    }
}
