package com.paulfvt.liftapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.paulfvt.liftapp.core.database.dao.ExerciseDao
import com.paulfvt.liftapp.core.database.entity.BadgeEntity
import com.paulfvt.liftapp.core.database.entity.BodyMeasurementEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseMuscleEntity
import com.paulfvt.liftapp.core.database.entity.GoalEntity
import com.paulfvt.liftapp.core.database.entity.PersonalRecordEntity
import com.paulfvt.liftapp.core.database.entity.ProgressPhotoEntity
import com.paulfvt.liftapp.core.database.entity.TemplateExerciseEntity
import com.paulfvt.liftapp.core.database.entity.TemplateFolderEntity
import com.paulfvt.liftapp.core.database.entity.WeeklyGoalEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutExerciseEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutSetEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutTemplateEntity

// Toute modification d'une entité impose d'augmenter version et d'écrire une migration testée.
// Le schéma de chaque version est exporté dans app/schemas/ (exportSchema = true par défaut).
@Database(
    version = 1,
    entities = [
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        TemplateFolderEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        PersonalRecordEntity::class,
        BodyMeasurementEntity::class,
        ProgressPhotoEntity::class,
        GoalEntity::class,
        WeeklyGoalEntity::class,
        BadgeEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class LiftDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        const val NAME = "lift.db"
    }
}
