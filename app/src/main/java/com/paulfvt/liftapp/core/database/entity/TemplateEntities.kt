package com.paulfvt.liftapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val setCount: Int,
    val targetReps: Int?,
    val restSeconds: Int?
)
