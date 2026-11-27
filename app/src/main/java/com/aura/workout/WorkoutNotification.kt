package com.aura.workout

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import coil.imageLoader
import coil.request.ImageRequest
import com.aura.AuraApp
import com.aura.MainActivity
import com.aura.R
import com.aura.data.local.dao.WorkoutWithSets
import com.aura.data.local.entity.WorkoutSetEntity
import com.aura.data.model.ExerciseLib
import com.aura.ui.screens.workout.WorkoutWeightUnit
import com.aura.ui.screens.workout.workoutNumber
import com.aura.ui.theme.V
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class WorkoutNotification(private val app: AuraApp) {
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val preferences = app.getSharedPreferences("workout_notifications", Context.MODE_PRIVATE)
    private val units = app.getSharedPreferences("workout_display_units", Context.MODE_PRIVATE)
    private val auth = FirebaseAuth.getInstance()
    private val owner = MutableStateFlow(auth.currentUser?.uid)
    private val updates = MutableStateFlow(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "Ongoing workout", NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) })
        auth.addAuthStateListener { owner.value = it.currentUser?.uid }
        scope.launch {
            combine(app.db.workoutDao().observeActiveWorkoutWithSets(), owner, updates) { session, uid, _ ->
                session?.takeIf { it.workout.cloudOwnerUid == null || it.workout.cloudOwnerUid == uid }
            }.collectLatest { session ->
                publish(session)
            }
        }
    }

    fun refresh() { updates.update { it + 1 } }

    fun shouldRequestPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !preferences.getBoolean("permission_requested", false) &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun markPermissionRequested() {
        preferences.edit().putBoolean("permission_requested", true).apply()
    }

    private fun canShow(workoutId: Long): Boolean =
        manager.areNotificationsEnabled() && preferences.getLong("dismissed_workout", -1) != workoutId

    private suspend fun publish(session: WorkoutWithSets?) {
        if (session == null || !canShow(session.workout.id)) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val workout = session.workout
        val next = nextSet(session.sets)
        val exercise = next?.let { ExerciseLib.getById(it.exerciseId) }
        val unit = if (units.getLong("pounds_workout", -1L) == workout.id) WorkoutWeightUnit.LB else WorkoutWeightUnit.KG
        val title = exercise?.name ?: next?.exerciseName?.takeIf { it.isNotBlank() } ?: workout.name
        val text = when {
            next == null && session.sets.isEmpty() -> "Open Aura to add an exercise"
            next == null -> "All sets complete · Open Aura to finish"
            !next.canComplete() -> "Set ${next.setNumber} · Enter reps in Aura"
            else -> {
                val total = session.sets.count { it.exerciseId == next.exerciseId }
                "Set ${next.setNumber}/$total · ${workoutNumber(unit.fromKilograms(next.weight))} ${unit.symbol} × ${next.reps} reps"
            }
        }
        val open = Intent(app, MainActivity::class.java).setAction(OPEN_WORKOUT)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val builder = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_workout_notification)
            .setColor(V.Teal.toArgb())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(PendingIntent.getActivity(app, 0, open, PendingIntent.FLAG_IMMUTABLE))
            .setDeleteIntent(action(DISMISS, workout.id))
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        if (next != null && next.canComplete()) {
            builder.addAction(R.drawable.ic_workout_notification, "Complete set", action(COMPLETE, workout.id, next.id))
        }
        manager.notify(NOTIFICATION_ID, builder.build())

        exercise?.imageUrl?.let { url ->
            val image = app.imageLoader.execute(
                ImageRequest.Builder(app).data(url).size(96).allowHardware(false).build(),
            ).drawable as? BitmapDrawable
            if (image != null && canShow(workout.id)) {
                manager.notify(NOTIFICATION_ID, builder.setLargeIcon(image.bitmap).build())
            }
        }
    }

    private fun action(action: String, workoutId: Long, setId: Long = -1): PendingIntent {
        val intent = Intent(app, WorkoutNotificationReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("aura-workout://notification/$workoutId/$setId"))
        return PendingIntent.getBroadcast(app, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun handle(action: String?, workoutId: Long, setId: Long, finished: () -> Unit) {
        scope.launch {
            try {
                when (action) {
                    DISMISS -> {
                        preferences.edit().putLong("dismissed_workout", workoutId).apply()
                        refresh()
                    }
                    COMPLETE -> {
                        val db = app.db
                        val dao = db.workoutDao()
                        db.withTransaction {
                            val workout = dao.getActiveWorkout() ?: return@withTransaction
                            if (workout.id != workoutId ||
                                (workout.cloudOwnerUid != null && workout.cloudOwnerUid != auth.currentUser?.uid)
                            ) return@withTransaction
                            val next = nextSet(dao.getWorkoutSetsList(workoutId))
                            if (next?.id == setId && next.canComplete()) {
                                dao.setCompletion(workoutId, setId, true, System.currentTimeMillis())
                            }
                        }
                        refresh()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.w("WorkoutNotification", "Could not apply notification action", exception)
            } finally {
                finished()
            }
        }
    }

    private fun nextSet(sets: List<WorkoutSetEntity>): WorkoutSetEntity? =
        sets.sortedBy { it.id }.groupBy { it.exerciseId }.values
            .firstNotNullOfOrNull { group -> group.filterNot { it.isCompleted }.minByOrNull { it.setNumber } }

    private fun WorkoutSetEntity.canComplete(): Boolean = reps > 0 && weight.isFinite() && weight >= 0

    companion object {
        const val OPEN_WORKOUT = "com.aura.workout.OPEN"
        private const val COMPLETE = "com.aura.workout.COMPLETE"
        private const val DISMISS = "com.aura.workout.DISMISS"
        private const val CHANNEL = "ongoing_workout"
        private const val NOTIFICATION_ID = 4401
    }
}

class WorkoutNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val segments = intent.data?.pathSegments ?: return
        val workoutId = segments.getOrNull(0)?.toLongOrNull() ?: return
        val setId = segments.getOrNull(1)?.toLongOrNull() ?: return
        val pending = goAsync()
        val app = context.applicationContext as AuraApp
        app.workoutNotification.handle(intent.action, workoutId, setId, pending::finish)
    }
}
