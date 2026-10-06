package com.paulfvt.liftapp.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.MuscleRole
import com.paulfvt.liftapp.core.model.TrackingType

// Index unique sur catalogKey : un exercice du catalogue n'existe qu'une fois. SQLite autorise plusieurs NULL,
// donc les exercices personnalisés (sans clé) ne sont pas concernés.
@Entity(tableName = "exercise", indices = [Index(value = ["catalogKey"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catalogKey: String?,
    val name: String,
    val equipment: Equipment,
    val category: ExerciseCategory,
    val trackingType: TrackingType,
    // Étapes séparées par un retour à la ligne.
    val instructions: String?,
    val imageCount: Int,
    val isCustom: Boolean,
    val isHidden: Boolean,
    val note: String?
)

@Entity(
    tableName = "exercise_muscle",
    primaryKeys = ["exerciseId", "muscle"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("muscle")]
)
data class ExerciseMuscleEntity(val exerciseId: Long, val muscle: Muscle, val role: MuscleRole)

// Résultat d'une requête qui charge un exercice avec ses muscles, en une seule transaction.
data class ExerciseWithMuscles(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val muscles: List<ExerciseMuscleEntity>
)
