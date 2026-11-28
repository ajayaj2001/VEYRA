package com.aura.data.cloud

import android.util.Log
import androidx.room.withTransaction
import com.aura.data.local.AppDatabase
import com.aura.data.local.entity.plan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class WorkoutCloudSync(private val database: () -> AppDatabase) {
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scanLock = Mutex()
    private val pending = mutableSetOf<Pair<String, Long>>()

    val ownerUid: String?
        get() = auth.currentUser?.uid

    init {
        auth.addAuthStateListener { sync() }
    }

    fun sync() {
        val uid = ownerUid ?: return
        scope.launch {
            try {
                scanLock.withLock {
                    if (uid == ownerUid) uploadPending(uid)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("WorkoutCloudSync", "Could not queue workout data", error)
            }
        }
    }

    private suspend fun uploadPending(uid: String) {
        val db = database()
        val dao = db.workoutDao()
        val uploads = db.withTransaction {
            val routines = dao.pendingRoutines(uid).map { routine ->
                val exercises = dao.getRoutineExercises(routine.id).map { exercise ->
                    mapOf(
                        "exerciseId" to exercise.exerciseId,
                        "sortOrder" to exercise.sortOrder,
                        "targetSets" to exercise.targetSets,
                        "targetReps" to exercise.targetReps,
                    ) + exercise.plan().cloudFields()
                }
                Upload("routines", checkNotNull(routine.cloudId), routine.updatedAt, mapOf(
                    "name" to routine.name,
                    "description" to routine.description,
                    "createdAtMs" to routine.createdAt,
                    "updatedAtMs" to routine.updatedAt,
                    "exercises" to exercises,
                )) { dao.markRoutineSynced(routine.id, routine.updatedAt) }
            }
            val workouts = dao.pendingWorkouts(uid).map { workout ->
                val endTime = checkNotNull(workout.endTime)
                val routineId = workout.routineId?.let { dao.getRoutineById(it) }
                    ?.takeIf { it.cloudOwnerUid == uid }?.cloudId
                val sets = dao.getWorkoutSetsList(workout.id)
                    .filter { it.isCompleted && it.reps > 0 }.map { set ->
                        mapOf(
                            "exerciseId" to set.exerciseId,
                            "exerciseName" to set.exerciseName,
                            "setNumber" to set.setNumber,
                            "weightKg" to set.weight,
                            "reps" to set.reps,
                            "isWarmup" to set.isWarmup,
                            "restSeconds" to set.restSeconds,
                            "timestampMs" to set.timestamp,
                        )
                    }
                Upload("workouts", checkNotNull(workout.cloudId), endTime, mapOf(
                    "name" to workout.name,
                    "routineId" to routineId,
                    "startedAtMs" to workout.startTime,
                    "endedAtMs" to endTime,
                    "durationSeconds" to workout.durationSeconds,
                    "volumeKg" to workout.totalVolume,
                    "notes" to workout.notes,
                    "sets" to sets,
                )) { dao.markWorkoutSynced(workout.id, endTime) }
            }
            val deletions = dao.pendingRoutineCloudDeletions(uid).map { deletion ->
                Upload("routines", deletion.cloudId, Long.MIN_VALUE, null) {
                    dao.acknowledgeRoutineCloudDeletion(deletion)
                }
            }
            routines + workouts + deletions
        }
        if (uid != ownerUid || uploads.isEmpty()) return

        val profile = FirebaseFirestore.getInstance().collection("profiles").document(uid)
        for (upload in uploads) {
            val document = profile.collection(upload.collection).document(upload.id)
            val key = document.path to upload.revision
            if (key in pending) continue

            val task = upload.data?.let { document.set(it + ("syncedAt" to FieldValue.serverTimestamp())) }
                ?: document.delete()
            pending.add(key)
            task.addOnCompleteListener { result ->
                scope.launch {
                    try {
                        if (result.isSuccessful) upload.acknowledge()
                        else Log.w("WorkoutCloudSync", "Firestore rejected workout data", result.exception)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("WorkoutCloudSync", "Could not record sync acknowledgement", error)
                    } finally {
                        pending.remove(key)
                    }
                }
            }
        }
    }

    private data class Upload(
        val collection: String,
        val id: String,
        val revision: Long,
        val data: Map<String, Any?>?,
        val acknowledge: suspend () -> Unit,
    )
}
