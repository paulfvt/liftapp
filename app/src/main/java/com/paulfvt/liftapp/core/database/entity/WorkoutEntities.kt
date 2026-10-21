package com.paulfvt.liftapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.paulfvt.liftapp.core.model.PersonalRecordType
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.WorkoutStatus
import java.time.Instant

// Supprimer un template ne supprime pas l'historique : la séance garde son nom et perd seulement le lien.
@Entity(
    tableName = "workout",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("templateId"), Index("startedAt"), Index("status")]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long?,
    val name: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val status: WorkoutStatus,
    val note: String?,
    // Repos en cours (version 4 du schéma) : la fin enregistrée sert de référence au minuteur, à la notification et à
    // l'alarme, ce qui le rend juste après une rotation, un arrêt de l'app ou un redémarrage. Null = pas de repos.
    val restStartedAt: Instant? = null,
    val restEndsAt: Instant? = null
)

// Pas de onDelete sur exerciseId : un exercice utilisé dans l'historique ne peut pas être supprimé.
@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"]
        )
    ],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val note: String?,
    // Repos repris du template (version 3 du schéma), utilisé par le minuteur. Null = pas de repos réglé.
    val restSeconds: Int?
)

// Valeurs en unités métriques : kg, secondes, mètres. RPE de 6 à 10 par demi-points.
@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val sortOrder: Int,
    val type: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
    val rpe: Double?,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    // Reps cibles reprises du template (version 3 du schéma), affichées pendant la séance.
    val targetReps: Int?
)

// Cache recalculable à partir des séries, jamais la source de vérité.
// weightKg n'est renseigné que pour MAX_REPS_AT_WEIGHT (le record de reps à cette charge).
@Entity(
    tableName = "personal_record",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId"), Index("setId")]
)
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val recordType: PersonalRecordType,
    val value: Double,
    val weightKg: Double?,
    val setId: Long,
    val achievedAt: Instant
)
