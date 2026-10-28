package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.SetType
import java.time.Instant
import java.time.ZoneId

// Badges du cahier des charges, dans l'ordre d'affichage. Le nom de chaque constante est le code enregistré en base
// (table badge) : ne jamais en renommer une. Aucun badge ne récompense le volume brut.
enum class Badge {
    WORKOUTS_1,
    WORKOUTS_10,
    WORKOUTS_50,
    WORKOUTS_100,
    STREAK_4,
    STREAK_12,
    STREAK_52,
    CLUB_BENCH_100,
    CLUB_SQUAT_140,
    CLUB_DEADLIFT_180
}

private val WORKOUT_MILESTONES = mapOf(
    Badge.WORKOUTS_1 to 1,
    Badge.WORKOUTS_10 to 10,
    Badge.WORKOUTS_50 to 50,
    Badge.WORKOUTS_100 to 100
)

// Semaines d'affilée avec l'objectif tenu (décision du 28 octobre 2026), c'est-à-dire la streak.
private val STREAK_MILESTONES = mapOf(Badge.STREAK_4 to 4, Badge.STREAK_12 to 12, Badge.STREAK_52 to 52)

// Club : une charge réellement soulevée, sur au moins une rep hors échauffement, sur un des mouvements classiques à la
// barre (décision du 28 octobre 2026, pas de Smith machine). Clés des exercices de free-exercise-db.
data class Club(val badge: Badge, val minKg: Double, val catalogKeys: Set<String>)

val CLUBS = listOf(
    Club(
        Badge.CLUB_BENCH_100,
        100.0,
        setOf("Barbell_Bench_Press_-_Medium_Grip", "Wide-Grip_Barbell_Bench_Press", "Bench_Press_-_Powerlifting")
    ),
    Club(Badge.CLUB_SQUAT_140, 140.0, setOf("Barbell_Squat", "Barbell_Full_Squat", "Wide_Stance_Barbell_Squat")),
    Club(Badge.CLUB_DEADLIFT_180, 180.0, setOf("Barbell_Deadlift", "Sumo_Deadlift"))
)

val CLUB_CATALOG_KEYS: List<String> = CLUBS.flatMap { it.catalogKeys }

// Badges que l'historique mérite, avec le moment où il les a mérités : début de la n-ième séance, séance qui a tenu
// l'objectif de la n-ième semaine d'affilée, séance du premier soulevé du club.
fun earnedBadges(
    sessionStarts: List<Instant>,
    weeklyGoals: List<WeeklyGoal>,
    clubHistories: List<ExerciseHistory>,
    zone: ZoneId
): Map<Badge, Instant> {
    val earned = mutableMapOf<Badge, Instant>()
    val starts = sessionStarts.sorted()
    WORKOUT_MILESTONES.forEach { (badge, count) -> starts.getOrNull(count - 1)?.let { earned[badge] = it } }
    val runs = streakRuns(reachedWeeks(starts, weeklyGoals, zone))
    STREAK_MILESTONES.forEach { (badge, weeks) ->
        runs.firstOrNull { it.size >= weeks }?.let { earned[badge] = it[weeks - 1].reachedAt }
    }
    CLUBS.forEach { club ->
        clubHistories.filter { it.exercise.catalogKey in club.catalogKeys }
            .flatMap { it.sets }
            .filter {
                it.type != SetType.WARMUP &&
                    (it.values.reps ?: 0) >= 1 &&
                    (it.values.weightKg ?: 0.0) >= club.minKg
            }
            .minOfOrNull { it.performedAt }
            ?.let { earned[club.badge] = it }
    }
    return earned
}
