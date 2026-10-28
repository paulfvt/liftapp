package com.paulfvt.liftapp.history.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.WorkoutExerciseEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutSetEntity
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.history.domain.editProblem
import com.paulfvt.liftapp.progress.data.AchievementRepository
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.workout.data.toDomain
import com.paulfvt.liftapp.workout.domain.Workout
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Séances terminées : historique, modification et suppression. Chaque modification recalcule les records des
// exercices touchés, dans la même transaction, puis vérifie paliers et badges.
@Singleton
class HistoryRepository @Inject constructor(
    private val database: LiftDatabase,
    private val records: RecordRepository,
    private val achievements: AchievementRepository
) {
    private val dao = database.workoutDao()

    fun observeFinished(): Flow<List<Workout>> =
        dao.observeAllByStatus(WorkoutStatus.FINISHED).map { rows -> rows.map { it.toDomain() } }

    // Null si la séance n'existe plus (supprimée) ou n'est pas terminée.
    fun observeWorkout(id: Long): Flow<Workout?> = dao.observeDetails(id).map { details ->
        details?.takeIf { it.workout.status == WorkoutStatus.FINISHED }?.toDomain()
    }

    suspend fun getWorkout(id: Long): Workout? =
        dao.getDetails(id)?.takeIf { it.workout.status == WorkoutStatus.FINISHED }?.toDomain()

    suspend fun delete(workoutId: Long) {
        database.withTransaction {
            val exerciseIds = dao.exerciseIdsOf(workoutId)
            dao.deleteWorkout(workoutId)
            records.recompute(exerciseIds)
            achievements.refresh()
        }
    }

    // Enregistre une séance modifiée. Ses exercices et séries sont réécrits en entier, comme pour un template ;
    // les séries déjà présentes gardent leur heure de validation. Les records sont recalculés pour les exercices
    // d'avant et d'après (un exercice retiré peut perdre un record).
    suspend fun save(edited: Workout): Unit = database.withTransaction {
        check(edited.editProblem() == null) { "Séance incomplète : ${edited.editProblem()}" }
        val before = dao.exerciseIdsOf(edited.id)
        val completedAt = dao.setCompletionsOf(edited.id).associate { it.id to it.completedAt }
        val fallbackCompletedAt = edited.endedAt ?: edited.startedAt
        dao.updateName(edited.id, edited.name.trim())
        dao.updateNote(edited.id, edited.note?.trim()?.ifEmpty { null })
        dao.deleteExercisesOf(edited.id)
        edited.exercises.forEachIndexed { index, item ->
            val exerciseRowId = dao.insertExercise(
                WorkoutExerciseEntity(
                    workoutId = edited.id,
                    exerciseId = item.exercise.id,
                    sortOrder = index,
                    note = null,
                    restSeconds = item.restSeconds
                )
            )
            dao.insertSets(
                item.sets.mapIndexed { setIndex, set ->
                    WorkoutSetEntity(
                        workoutExerciseId = exerciseRowId,
                        sortOrder = setIndex,
                        type = set.type,
                        weightKg = set.values.weightKg,
                        reps = set.values.reps,
                        durationSec = set.values.durationSec,
                        distanceM = set.values.distanceM,
                        rpe = set.rpe,
                        isCompleted = true,
                        completedAt = completedAt[set.id] ?: fallbackCompletedAt,
                        targetReps = set.targetReps
                    )
                }
            )
        }
        records.recompute(before + edited.exercises.map { it.exercise.id })
        // Une séance corrigée à la hausse peut atteindre un palier ou débloquer un badge (acquis pour toujours).
        achievements.refresh()
        Unit
    }
}
