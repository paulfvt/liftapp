package com.paulfvt.liftapp.workout.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.WorkoutEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutExerciseEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutSetEntity
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.progress.data.AchievementRepository
import com.paulfvt.liftapp.progress.data.Achievements
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.progress.domain.RecordBreak
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.template.domain.DEFAULT_REST_SECONDS
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.workout.domain.PlannedSet
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutSummary
import com.paulfvt.liftapp.workout.domain.addedSet
import com.paulfvt.liftapp.workout.domain.adjusted
import com.paulfvt.liftapp.workout.domain.canComplete
import com.paulfvt.liftapp.workout.domain.differsFrom
import com.paulfvt.liftapp.workout.domain.prefill
import com.paulfvt.liftapp.workout.domain.restAfterCompleting
import com.paulfvt.liftapp.workout.domain.setsForAddedExercise
import com.paulfvt.liftapp.workout.domain.summarize
import com.paulfvt.liftapp.workout.domain.toTemplateDraft
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Résultat d'une fin de séance : le résumé, les records battus, le tonnage de la dernière fois avec le même
// template, et le nouveau contenu du template si la séance l'a modifié.
data class FinishedWorkout(
    val name: String,
    val summary: WorkoutSummary,
    val templateId: Long?,
    val templateUpdate: TemplateDraft?,
    val records: List<FinishedRecord> = emptyList(),
    val previousTonnageKg: Double? = null,
    val previousStartedAt: Instant? = null,
    val achievements: Achievements = Achievements()
)

data class FinishedRecord(val exercise: Exercise, val recordBreak: RecordBreak)

// Unique point d'accès aux séances. Une seule séance peut être en cours à la fois.
@Singleton
class WorkoutRepository @Inject constructor(
    private val database: LiftDatabase,
    private val templates: TemplateRepository,
    private val records: RecordRepository,
    private val achievements: AchievementRepository,
    private val clock: Clock
) {
    private val dao = database.workoutDao()

    fun observeInProgress(): Flow<Workout?> = dao.observeByStatus(WorkoutStatus.IN_PROGRESS).map { it?.toDomain() }

    suspend fun inProgressId(): Long? = dao.firstIdWithStatus(WorkoutStatus.IN_PROGRESS)

    // Dernière fois sur cet exercice : dans le même template d'abord, sinon tous templates confondus.
    suspend fun previousSets(exerciseId: Long, templateId: Long?): List<PreviousSet> {
        val workoutExerciseId = templateId?.let { dao.lastWorkoutExerciseId(exerciseId, it, WorkoutStatus.FINISHED) }
            ?: dao.lastWorkoutExerciseId(exerciseId, null, WorkoutStatus.FINISHED)
            ?: return emptyList()
        return dao.getSets(workoutExerciseId).filter { it.isCompleted }.map { PreviousSet(it.type, it.toValues()) }
    }

    // Les séries du template sont créées avec leur type et leur cible, pré-remplies avec la dernière fois.
    suspend fun startFromTemplate(templateId: Long): Long = database.withTransaction {
        check(inProgressId() == null) { "Une séance est déjà en cours" }
        val draft = requireNotNull(templates.getDraft(templateId)) { "Template $templateId introuvable" }
        val workoutId = insertWorkout(templateId, draft.name)
        draft.exercises.forEachIndexed { index, item ->
            val planned = item.sets.map { PlannedSet(it.type, it.targetReps) }
            insertExercise(
                workoutId,
                item.exercise.id,
                index,
                item.restSeconds,
                prefill(planned, previousSets(item.exercise.id, templateId))
            )
        }
        workoutId
    }

    suspend fun startEmpty(name: String): Long = database.withTransaction {
        check(inProgressId() == null) { "Une séance est déjà en cours" }
        insertWorkout(templateId = null, name = name)
    }

    suspend fun addExercises(workoutId: Long, exerciseIds: List<Long>) = database.withTransaction {
        val templateId = requireNotNull(dao.getWorkout(workoutId)).templateId
        val firstOrder = (dao.getExercises(workoutId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        exerciseIds.forEachIndexed { index, exerciseId ->
            val planned = setsForAddedExercise(previousSets(exerciseId, templateId))
            insertExercise(workoutId, exerciseId, firstOrder + index, DEFAULT_REST_SECONDS, planned)
        }
    }

    // Remplacer garde les séries prévues (types et cibles) et les pré-remplit avec le nouvel exercice.
    suspend fun replaceExercise(workoutExerciseId: Long, exerciseId: Long) = database.withTransaction {
        val current = requireNotNull(dao.getExercise(workoutExerciseId))
        val templateId = requireNotNull(dao.getWorkout(current.workoutId)).templateId
        val planned = dao.getSets(workoutExerciseId).map { PlannedSet(it.type, it.targetReps) }
        dao.updateExerciseExercise(workoutExerciseId, exerciseId)
        dao.deleteSetsOf(workoutExerciseId)
        dao.insertSets(prefill(planned, previousSets(exerciseId, templateId)).toEntities(workoutExerciseId))
    }

    suspend fun removeExercise(workoutExerciseId: Long) = dao.deleteExercise(workoutExerciseId)

    suspend fun reorderExercises(orderedIds: List<Long>) = database.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.updateExerciseOrder(id, index) }
    }

    suspend fun addSet(workoutExerciseId: Long) = database.withTransaction {
        val exercise = requireNotNull(dao.getExercise(workoutExerciseId))
        val templateId = requireNotNull(dao.getWorkout(exercise.workoutId)).templateId
        val sets = dao.getSets(workoutExerciseId)
        val planned = addedSet(sets.map { it.type }, previousSets(exercise.exerciseId, templateId))
        val nextOrder = (sets.maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.insertSets(listOf(planned).toEntities(workoutExerciseId, firstOrder = nextOrder))
    }

    suspend fun removeSet(setId: Long) = dao.deleteSet(setId)

    suspend fun updateSetType(setId: Long, type: SetType) = dao.updateSetType(setId, type)

    suspend fun updateSetWeight(setId: Long, weightKg: Double?) = dao.updateSetWeight(setId, weightKg)

    suspend fun updateSetReps(setId: Long, reps: Int?) = dao.updateSetReps(setId, reps)

    suspend fun updateSetDuration(setId: Long, durationSec: Int?) = dao.updateSetDuration(setId, durationSec)

    suspend fun updateSetDistance(setId: Long, distanceM: Double?) = dao.updateSetDistance(setId, distanceM)

    suspend fun updateSetRpe(setId: Long, rpe: Double?) = dao.updateSetRpe(setId, rpe)

    // Boutons − et + du mode salle : le pas est appliqué à la valeur en base, pas à celle affichée, pour que des
    // appuis rapides s'additionnent même si l'écran n'a pas encore reçu la valeur précédente.
    suspend fun changeSetValues(setId: Long, change: (SetValues) -> SetValues) = database.withTransaction {
        val set = dao.getSet(setId) ?: return@withTransaction
        val values = change(set.toValues())
        dao.updateSetValues(setId, values.weightKg, values.reps, values.durationSec, values.distanceM)
    }

    suspend fun setCompleted(setId: Long, isCompleted: Boolean) =
        dao.updateSetCompleted(setId, isCompleted, if (isCompleted) clock.instant() else null)

    // Coche ou décoche une série. Valider lance le repos de l'exercice ; décocher ne touche pas au repos en cours.
    // La vérification se fait sur la valeur en base, donc après les saisies en attente : renvoie false (sans rien
    // changer) si la série ne peut pas être validée faute de valeur.
    suspend fun toggleCompleted(setId: Long): Boolean = database.withTransaction {
        val set = dao.getSet(setId) ?: return@withTransaction true
        if (set.isCompleted) {
            setCompleted(setId, false)
            true
        } else {
            completeAndStartRest(set)
        }
    }

    // Action « Série validée » de la notification : valide la série affichée si elle ne l'est pas déjà.
    suspend fun completeSet(setId: Long): Boolean = database.withTransaction {
        val set = dao.getSet(setId) ?: return@withTransaction false
        set.isCompleted || completeAndStartRest(set)
    }

    // « +30 s » et « −15 s » sur le repos de la séance en cours.
    suspend fun adjustRest(deltaSeconds: Int) = database.withTransaction {
        val workout = inProgressId()?.let { dao.getWorkout(it) } ?: return@withTransaction
        val rest = workout.restPeriod().adjusted(deltaSeconds, clock.instant())
        dao.updateRest(workout.id, rest?.startedAt, rest?.endsAt)
    }

    suspend fun skipRest() {
        inProgressId()?.let { dao.updateRest(it, null, null) }
    }

    private suspend fun completeAndStartRest(set: WorkoutSetEntity): Boolean {
        val workoutId = requireNotNull(dao.getExercise(set.workoutExerciseId)).workoutId
        val workout = requireNotNull(dao.getDetails(workoutId)).toDomain()
        val exercise = workout.exercises.first { it.id == set.workoutExerciseId }.exercise
        if (!canComplete(set.toValues(), exercise.trackingType)) return false
        val now = clock.instant()
        dao.updateSetCompleted(set.id, true, now)
        val rest = workout.restAfterCompleting(set.id, now)
        dao.updateRest(workoutId, rest?.startedAt, rest?.endsAt)
        return true
    }

    suspend fun rename(workoutId: Long, name: String) = dao.updateName(workoutId, name.trim())

    // Une note vide est enregistrée comme absente.
    suspend fun updateNote(workoutId: Long, note: String) = dao.updateNote(workoutId, note.trim().ifEmpty { null })

    // Fin de séance : la structure est comparée au template avant de retirer les séries non validées,
    // pour que seules les modifications volontaires proposent une mise à jour du template.
    suspend fun finish(workoutId: Long): FinishedWorkout = database.withTransaction {
        val workout = requireNotNull(dao.getDetails(workoutId)).toDomain()
        val endedAt = clock.instant()
        val template = workout.templateId?.let { templates.getDraft(it) }
        val templateUpdate = template?.takeIf { workout.differsFrom(it) }?.let { workout.toTemplateDraft(it.name) }
        dao.deleteIncompleteSets(workoutId)
        dao.deleteExercisesWithoutSets(workoutId)
        dao.updateStatus(workoutId, WorkoutStatus.FINISHED, endedAt)
        dao.updateRest(workoutId, null, null)
        records.recompute(workout.exercises.map { it.exercise.id })
        val previous = workout.templateId
            ?.let { dao.previousIdOfTemplate(it, workout.startedAt, WorkoutStatus.FINISHED) }
            ?.let { dao.getDetails(it)?.toDomain() }
        FinishedWorkout(
            name = workout.name,
            summary = summarize(workout, endedAt),
            templateId = workout.templateId,
            templateUpdate = templateUpdate,
            records = brokenRecords(workout),
            previousTonnageKg = previous?.let { summarize(it, it.endedAt ?: it.startedAt).tonnageKg },
            previousStartedAt = previous?.startedAt,
            achievements = achievements.refresh()
        )
    }

    // Records battus par cette séance, dans l'ordre des exercices de la séance.
    private suspend fun brokenRecords(workout: Workout): List<FinishedRecord> {
        val breaks = records.recordBreaks(workout.exercises.map { it.exercise.id })
        return workout.exercises.flatMap { item ->
            breaks[item.exercise.id].orEmpty()
                .filter { it.workoutId == workout.id }
                .map { FinishedRecord(item.exercise, it) }
        }
    }

    suspend fun updateTemplate(templateId: Long, draft: TemplateDraft) = templates.updateTemplate(templateId, draft)

    // Une séance abandonnée est gardée avec ce statut mais n'apparaît ni dans l'historique ni dans « Précédent ».
    suspend fun discard(workoutId: Long) = database.withTransaction {
        dao.updateStatus(workoutId, WorkoutStatus.DISCARDED, clock.instant())
        dao.updateRest(workoutId, null, null)
    }

    private suspend fun insertWorkout(templateId: Long?, name: String): Long = dao.insertWorkout(
        WorkoutEntity(
            templateId = templateId,
            name = name,
            startedAt = clock.instant(),
            endedAt = null,
            status = WorkoutStatus.IN_PROGRESS,
            note = null
        )
    )

    private suspend fun insertExercise(
        workoutId: Long,
        exerciseId: Long,
        sortOrder: Int,
        restSeconds: Int?,
        sets: List<PlannedSet>
    ) {
        val id = dao.insertExercise(
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                sortOrder = sortOrder,
                note = null,
                restSeconds = restSeconds
            )
        )
        dao.insertSets(sets.toEntities(id))
    }
}
