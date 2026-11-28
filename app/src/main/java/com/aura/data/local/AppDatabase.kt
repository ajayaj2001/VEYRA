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
        RoutineCloudDeletion::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        FoodItemEntity::class,
        FoodLogEntity::class,
        NutritionGoalEntity::class,
    ],
    version = 6,
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

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS routine_cloud_deletions (" +
                        "ownerUid TEXT NOT NULL, cloudId TEXT NOT NULL, PRIMARY KEY(ownerUid, cloudId))",
                )
            }
        }

        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN planJson TEXT")
            }
        }

        private val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN nutrientsJson TEXT")
                db.execSQL("ALTER TABLE food_logs ADD COLUMN nutrientsJson TEXT")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "aura_db"
                ).addMigrations(migration2To3, migration3To4, migration4To5, migration5To6).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
