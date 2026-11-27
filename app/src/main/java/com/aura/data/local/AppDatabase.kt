package com.aura.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aura.data.local.dao.NutritionDao
import com.aura.data.local.dao.WorkoutDao
import com.aura.data.local.entity.*

@Database(
    entities = [
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        FoodItemEntity::class,
        FoodLogEntity::class,
        NutritionGoalEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("routines", "workouts")) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN cloudOwnerUid TEXT")
                    db.execSQL("ALTER TABLE $table ADD COLUMN cloudId TEXT")
                    db.execSQL("ALTER TABLE $table ADD COLUMN cloudSyncedRevision INTEGER")
                }
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "aura_db"
                ).addMigrations(migration2To3).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
