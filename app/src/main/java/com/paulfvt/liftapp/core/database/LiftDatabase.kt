package com.paulfvt.liftapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.paulfvt.liftapp.core.database.dao.ExerciseDao
import com.paulfvt.liftapp.core.database.dao.MeasurementDao
import com.paulfvt.liftapp.core.database.dao.MotivationDao
import com.paulfvt.liftapp.core.database.dao.PhotoDao
import com.paulfvt.liftapp.core.database.dao.RecordDao
import com.paulfvt.liftapp.core.database.dao.TemplateDao
import com.paulfvt.liftapp.core.database.dao.WorkoutDao
import com.paulfvt.liftapp.core.database.entity.BadgeEntity
import com.paulfvt.liftapp.core.database.entity.BodyMeasurementEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseMuscleEntity
import com.paulfvt.liftapp.core.database.entity.GoalEntity
import com.paulfvt.liftapp.core.database.entity.PersonalRecordEntity
import com.paulfvt.liftapp.core.database.entity.ProgressPhotoEntity
import com.paulfvt.liftapp.core.database.entity.TemplateExerciseEntity
import com.paulfvt.liftapp.core.database.entity.TemplateFolderEntity
import com.paulfvt.liftapp.core.database.entity.TemplateSetEntity
import com.paulfvt.liftapp.core.database.entity.WeeklyGoalEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutExerciseEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutSetEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutTemplateEntity
import com.paulfvt.liftapp.core.database.migration.ALL_MIGRATIONS

// Toute modification d'une entité impose d'augmenter cette version et d'écrire une migration testée.
// Le schéma de chaque version est exporté dans app/schemas/ (exportSchema = true par défaut).
const val DATABASE_VERSION = 4

@Database(
    version = DATABASE_VERSION,
    entities = [
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        TemplateFolderEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        TemplateSetEntity::class,
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

    abstract fun templateDao(): TemplateDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun recordDao(): RecordDao

    abstract fun measurementDao(): MeasurementDao

    abstract fun photoDao(): PhotoDao

    abstract fun motivationDao(): MotivationDao

    companion object {
        const val NAME = "lift.db"

        // Pas de fallbackToDestructiveMigration() : il effacerait les données des utilisateurs. name peut être un
        // chemin complet, pour ouvrir une copie (sauvegarde à vérifier avant de la restaurer).
        fun build(context: Context, name: String = NAME): LiftDatabase =
            Room.databaseBuilder(context, LiftDatabase::class.java, name)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
