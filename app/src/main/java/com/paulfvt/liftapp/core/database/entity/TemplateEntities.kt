package com.paulfvt.liftapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.paulfvt.liftapp.core.model.SetType

@Entity(tableName = "template_folder")
data class TemplateFolderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val sortOrder: Int)

// Supprimer un dossier ne supprime pas ses templates : ils reviennent à la racine (SET_NULL).
@Entity(
    tableName = "workout_template",
    foreignKeys = [
        ForeignKey(
            entity = TemplateFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId")]
)
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long?,
    val name: String,
    val sortOrder: Int
)

@Entity(
    tableName = "template_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"]
        )
    ],
    indices = [Index("templateId"), Index("exerciseId")]
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val restSeconds: Int?
)

// Série prévue d'un exercice de template (depuis la version 2 du schéma). targetReps null = pas de cible.
@Entity(
    tableName = "template_set",
    foreignKeys = [
        ForeignKey(
            entity = TemplateExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateExerciseId")]
)
data class TemplateSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateExerciseId: Long,
    val sortOrder: Int,
    val type: SetType,
    val targetReps: Int?
)
