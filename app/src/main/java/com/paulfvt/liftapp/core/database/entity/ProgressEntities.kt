package com.paulfvt.liftapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.GoalMetric
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.model.PhotoPose
import java.time.Instant
import java.time.LocalDate

// Chaque saisie crée une nouvelle ligne : on n'écrase jamais une ancienne mesure.
// method n'est renseigné que pour le taux de masse grasse.
@Entity(tableName = "body_measurement", indices = [Index("type", "measuredAt")])
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MeasurementType,
    val value: Double,
    val method: BodyFatMethod?,
    val measuredAt: Instant
)

// filePath est relatif au dossier privé photos/ de l'app.
@Entity(tableName = "progress_photo", indices = [Index("takenAt")])
data class ProgressPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val pose: PhotoPose,
    val takenAt: Instant
)

@Entity(
    tableName = "goal",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val metric: GoalMetric,
    val bronzeValue: Double,
    val silverValue: Double,
    val goldValue: Double,
    val createdAt: Instant,
    val bronzeAchievedAt: Instant?,
    val silverAchievedAt: Instant?,
    val goldAchievedAt: Instant?
)

// Objectif historisé : chaque changement crée une ligne, valable à partir du lundi effectiveFrom.
// Les semaines passées gardent ainsi l'objectif de l'époque pour le calcul de la streak.
@Entity(tableName = "weekly_goal", indices = [Index(value = ["effectiveFrom"], unique = true)])
data class WeeklyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: Int,
    val effectiveFrom: LocalDate
)

// Un badge débloqué reste acquis, même si l'historique est modifié ensuite.
@Entity(tableName = "badge")
data class BadgeEntity(@PrimaryKey val code: String, val unlockedAt: Instant)
