package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.workout.domain.setTonnageKg
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToLong

// Objets de comparaison pour rendre le tonnage parlant, du plus léger au plus lourd (masses approchées).
enum class TonnageObject(val massKg: Double) {
    CEMENT_BAG(25.0),
    UPRIGHT_PIANO(250.0),
    CITY_CAR(1_200.0),
    ELEPHANT(6_000.0),
    BLUE_WHALE(150_000.0)
}

data class TonnageEquivalence(val thing: TonnageObject, val count: Double)

private const val ROUND_TO_TENTHS_BELOW = 10

// L'objet le plus lourd dont le tonnage fait au moins un exemplaire : « 8 éléphants » plutôt que « 40 voitures ».
// Nombre arrondi au dixième sous 10, à l'unité au-delà. Rien sous un sac de ciment.
fun tonnageEquivalence(tonnageKg: Double): TonnageEquivalence? {
    val thing = TonnageObject.entries.lastOrNull { tonnageKg >= it.massKg } ?: return null
    val count = tonnageKg / thing.massKg
    val rounded = if (count <
        ROUND_TO_TENTHS_BELOW
    ) {
        (count * 10).roundToLong() / 10.0
    } else {
        count.roundToLong().toDouble()
    }
    return TonnageEquivalence(thing, rounded)
}

// Record du mois : la meilleure valeur atteinte dans le mois pour une mesure d'un exercice.
data class RecapRecord(val exercise: Exercise, val metric: ExerciseMetric, val value: Double, val achievedAt: Instant)

data class FavoriteExercise(val exercise: Exercise, val sessions: Int)

data class MonthlyRecap(
    val month: YearMonth,
    val workoutCount: Int,
    val tonnageKg: Double,
    val equivalence: TonnageEquivalence?,
    val records: List<RecapRecord>,
    val favorite: FavoriteExercise?
)

// Récap d'un mois, dans le fuseau du téléphone (une séance compte pour le mois de son début). Les records sont ceux
// battus pendant le mois, une ligne par exercice et par mesure (la dernière valeur, la meilleure). L'exercice favori
// est celui fait dans le plus de séances, puis avec le plus de séries.
fun monthlyRecap(histories: List<ExerciseHistory>, month: YearMonth, zone: ZoneId): MonthlyRecap {
    fun inMonth(instant: Instant) = YearMonth.from(instant.atZone(zone)) == month
    val monthly = histories.map { history -> history to history.sets.filter { inMonth(it.performedAt) } }
        .filter { (_, sets) -> sets.isNotEmpty() }
    val tonnage = monthly.sumOf { (history, sets) ->
        sets.sumOf { setTonnageKg(history.exercise.trackingType, it.type, it.values) }
    }
    val records = histories.flatMap { (exercise, sets) ->
        recordBreaks(sets, exercise.trackingType)
            .filter { inMonth(it.achievedAt) }
            .groupBy { it.metric }
            .map { (metric, breaks) -> breaks.last().let { RecapRecord(exercise, metric, it.value, it.achievedAt) } }
    }.sortedByDescending { it.achievedAt }
    val favorite = monthly
        .map { (history, sets) ->
            val effective = sets.count { it.isEffective(history.exercise.trackingType) }
            Triple(history.exercise, sets.map { it.workoutId }.distinct().size, effective)
        }
        .sortedWith(
            compareByDescending<Triple<Exercise, Int, Int>> { it.second }
                .thenByDescending { it.third }
                .thenBy { it.first.name }
        )
        .firstOrNull()
        ?.let { (exercise, sessions, _) -> FavoriteExercise(exercise, sessions) }
    return MonthlyRecap(
        month = month,
        workoutCount = monthly.flatMap { (_, sets) -> sets.map { it.workoutId } }.distinct().size,
        tonnageKg = tonnage,
        equivalence = tonnageEquivalence(tonnage),
        records = records,
        favorite = favorite
    )
}

// Mois proposés dans le récap : ceux qui ont au moins une séance, plus le mois en cours, du plus récent au plus ancien.
fun recapMonths(histories: List<ExerciseHistory>, current: YearMonth, zone: ZoneId): List<YearMonth> =
    (histories.flatMap { history -> history.sets.map { YearMonth.from(it.performedAt.atZone(zone)) } } + current)
        .distinct()
        .sortedDescending()
