package com.aura

import android.app.Application
import com.aura.data.cloud.WorkoutCloudSync
import com.aura.data.local.AppDatabase
import com.aura.data.model.ExerciseLib
import com.aura.workout.WorkoutNotification

class AuraApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.getInstance(this) }

    internal val workoutCloud by lazy { WorkoutCloudSync { db } }

    internal val workoutNotification by lazy { WorkoutNotification(this) }

    override fun onCreate() {
        super.onCreate()
        ExerciseLib.init(this)
        workoutCloud.sync()
    }
}
