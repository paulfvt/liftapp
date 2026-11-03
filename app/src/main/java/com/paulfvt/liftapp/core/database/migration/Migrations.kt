package com.paulfvt.liftapp.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Version 2 : les séries d'un template sont détaillées une par une (table template_set) au lieu d'un nombre de
// séries et d'une cible de reps communs. « 4 séries de 8 » devient 4 séries de travail à 8 reps.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. On lit les anciennes valeurs avant de recréer la table qui les contient.
        class OldExercise(val id: Long, val setCount: Int, val targetReps: Int?)
        val oldExercises = mutableListOf<OldExercise>()
        db.query("SELECT id, setCount, targetReps FROM template_exercise").use { cursor ->
            while (cursor.moveToNext()) {
                val targetReps = if (cursor.isNull(2)) null else cursor.getInt(2)
                oldExercises += OldExercise(cursor.getLong(0), cursor.getInt(1), targetReps)
            }
        }

        // 2. SQLite d'Android 8 ne sait pas supprimer une colonne : on recrée la table sans setCount ni targetReps,
        // en gardant les identifiants. Les instructions sont celles du schéma exporté (app/schemas/.../2.json).
        db.execSQL(
            "CREATE TABLE `template_exercise_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`templateId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                "`restSeconds` INTEGER, " +
                "FOREIGN KEY(`templateId`) REFERENCES `workout_template`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        db.execSQL(
            "INSERT INTO template_exercise_new (id, templateId, exerciseId, sortOrder, restSeconds) " +
                "SELECT id, templateId, exerciseId, sortOrder, restSeconds FROM template_exercise"
        )
        db.execSQL("DROP TABLE template_exercise")
        db.execSQL("ALTER TABLE template_exercise_new RENAME TO template_exercise")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_template_exercise_templateId` ON `template_exercise` (`templateId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_template_exercise_exerciseId` ON `template_exercise` (`exerciseId`)"
        )

        // 3. Nouvelle table des séries, créée après la recréation ci-dessus : supprimer l'ancienne table aurait
        // sinon effacé ses séries par la clé étrangère ON DELETE CASCADE.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `template_set` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`templateExerciseId` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                "`targetReps` INTEGER, " +
                "FOREIGN KEY(`templateExerciseId`) REFERENCES `template_exercise`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_template_set_templateExerciseId` ON `template_set` (`templateExerciseId`)"
        )
        oldExercises.forEach { exercise ->
            repeat(exercise.setCount) { index ->
                db.execSQL(
                    "INSERT INTO template_set (templateExerciseId, sortOrder, type, targetReps) VALUES (?, ?, 'WORK', ?)",
                    arrayOf<Any?>(exercise.id, index, exercise.targetReps)
                )
            }
        }
    }
}

// Version 3 : la séance garde le repos et les reps cibles de son template. Ajouter une colonne suffit ici,
// les lignes existantes reçoivent NULL.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_exercise ADD COLUMN restSeconds INTEGER")
        db.execSQL("ALTER TABLE workout_set ADD COLUMN targetReps INTEGER")
    }
}

// Version 4 : la séance retient le début et la fin du repos en cours, pour le minuteur et la notification.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout ADD COLUMN restStartedAt INTEGER")
        db.execSQL("ALTER TABLE workout ADD COLUMN restEndsAt INTEGER")
    }
}

// Version 5 : nutrition (phase 12). Nouvelles tables seulement, rien ne change dans les tables existantes. Les
// instructions sont celles du schéma exporté (app/schemas/.../5.json). Les aliments de la table Ciqual sont ajoutés
// ensuite par FoodImporter, comme le catalogue d'exercices.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`ciqualCode` INTEGER, `name` TEXT NOT NULL, `kcal` REAL NOT NULL, `protein` REAL NOT NULL, " +
                "`carbs` REAL NOT NULL, `fat` REAL NOT NULL, `isFavorite` INTEGER NOT NULL, `isHidden` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_ciqualCode` ON `food` (`ciqualCode`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` INTEGER NOT NULL, `meal` TEXT NOT NULL, `foodId` INTEGER NOT NULL, `grams` REAL NOT NULL, " +
                "`kcal` REAL NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, `fat` REAL NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`foodId`) REFERENCES `food`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_entry_date` ON `food_entry` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_entry_foodId` ON `food_entry` (`foodId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_meal` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_meal_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`savedMealId` INTEGER NOT NULL, `foodId` INTEGER NOT NULL, `grams` REAL NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, " +
                "FOREIGN KEY(`savedMealId`) REFERENCES `saved_meal`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`foodId`) REFERENCES `food`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_saved_meal_item_savedMealId` ON `saved_meal_item` (`savedMealId`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_meal_item_foodId` ON `saved_meal_item` (`foodId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `nutrition_goal` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`effectiveFrom` INTEGER NOT NULL, `kcal` INTEGER NOT NULL, `proteinGrams` INTEGER NOT NULL, " +
                "`carbsGrams` INTEGER NOT NULL, `fatGrams` INTEGER NOT NULL, `activityLevel` TEXT NOT NULL, " +
                "`objective` TEXT NOT NULL, `proteinPerKg` REAL NOT NULL, `fatPerKg` REAL NOT NULL, " +
                "`isKcalManual` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_nutrition_goal_effectiveFrom` ON `nutrition_goal` (`effectiveFrom`)"
        )
    }
}

// Toutes les migrations, pour la base de l'app et pour la vérification d'une sauvegarde à restaurer. En fin de
// fichier : les propriétés sont initialisées dans l'ordre, les migrations doivent déjà exister.
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
