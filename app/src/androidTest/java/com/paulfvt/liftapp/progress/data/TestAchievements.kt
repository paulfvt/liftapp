package com.paulfvt.liftapp.progress.data

import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import java.time.Clock

// Vérification des paliers et badges construite à la main pour les tests (Hilt n'est pas utilisé dans les tests).
fun testAchievements(database: LiftDatabase, exercises: ExerciseRepository, clock: Clock) =
    AchievementRepository(GoalRepository(database, exercises, clock), BadgeRepository(database, exercises))
