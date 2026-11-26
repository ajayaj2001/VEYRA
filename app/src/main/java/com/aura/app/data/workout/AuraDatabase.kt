package com.aura.app.data.workout

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aura.app.data.food.FoodDao
import com.aura.app.data.food.FoodEntryEntity
import com.aura.app.data.food.FoodIngredientEntity

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        FoodEntryEntity::class,
        FoodIngredientEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile private var instance: AuraDatabase? = null

        fun get(context: Context): AuraDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AuraDatabase::class.java,
                "aura.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        loggedAtEpochMs INTEGER NOT NULL,
                        amountGrams REAL NOT NULL,
                        calories INTEGER NOT NULL,
                        carbsGrams REAL NOT NULL,
                        proteinGrams REAL NOT NULL,
                        fatGrams REAL NOT NULL,
                        healthScore INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_entries_loggedAtEpochMs ON food_entries(loggedAtEpochMs)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_ingredients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        foodEntryId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        amountGrams REAL NOT NULL,
                        calories INTEGER NOT NULL,
                        carbsGrams REAL NOT NULL,
                        proteinGrams REAL NOT NULL,
                        fatGrams REAL NOT NULL,
                        position INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_ingredients_foodEntryId ON food_ingredients(foodEntryId)")
            }
        }
    }
}
