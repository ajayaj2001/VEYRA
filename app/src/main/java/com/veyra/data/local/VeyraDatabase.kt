package com.veyra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.veyra.data.local.dao.NutritionDao
import com.veyra.data.local.dao.WorkoutDao
import com.veyra.data.local.entity.FoodItemEntity
import com.veyra.data.local.entity.FoodLogEntity
import com.veyra.data.local.entity.MealEntity
import com.veyra.data.local.entity.MealItemEntity
import com.veyra.data.local.entity.NutritionGoalEntity
import com.veyra.data.local.entity.RoutineEntity
import com.veyra.data.local.entity.RoutineExerciseEntity
import com.veyra.data.local.entity.WorkoutEntity
import com.veyra.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        FoodItemEntity::class,
        FoodLogEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        NutritionGoalEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class VeyraDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        @Volatile
        private var INSTANCE: VeyraDatabase? = null

        fun getInstance(context: Context): VeyraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VeyraDatabase::class.java,
                    "veyra_database",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
