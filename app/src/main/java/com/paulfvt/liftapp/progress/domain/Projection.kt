package com.paulfvt.liftapp.progress.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sqrt

// Conditions (décision du 28 octobre 2026) : au moins 6 séances étalées sur 4 semaines ou plus, la dernière il y a
// moins de 4 semaines. Seuls les 6 derniers mois comptent : les débuts, très rapides, fausseraient la tendance.
const val PROJECTION_MIN_SESSIONS = 6
private const val MIN_SPAN_DAYS = 28L
private const val MAX_AGE_DAYS = 28L
private const val WINDOW_DAYS = 182L

// Horizon de la projection : environ 3 mois après aujourd'hui.
private const val HORIZON_DAYS = 90L

// Échelle de temps du modèle : x = ln(1 + jours / 30). La courbe monte de moins en moins vite, comme une progression
// réelle, au lieu de prolonger une droite.
private const val TIME_SCALE_DAYS = 30.0

// Fourchette d'environ 80 % autour de la courbe, jamais plus étroite que ±1,25 kg.
private const val SPREAD = 1.28
private const val MIN_HALF_WIDTH_KG = 1.25

// Au-delà d'un écart de ±10 % autour de la valeur projetée, les séances sont trop irrégulières pour projeter.
private const val MAX_RELATIVE_HALF_WIDTH = 0.10

private const val DISPLAY_STEP_KG = 0.5

data class Projection(val horizon: LocalDate, val lowKg: Double, val highKg: Double)

sealed interface ProjectionResult {
    data class Available(val projection: Projection) : ProjectionResult

    data object NotEnoughData : ProjectionResult

    data object NoProgress : ProjectionResult

    data object TooIrregular : ProjectionResult
}

// Projection du 1RM estimé à partir du meilleur 1RM de chaque séance (sessions, dans l'ordre). Régression des
// moindres carrés de y = a + b × ln(1 + t / 30), t en jours depuis la première séance retenue. Le résultat est une
// fourchette et une période (« vers fin janvier »), jamais une date précise présentée comme certaine.
fun projectOneRepMax(sessions: List<ProgressPoint>, today: LocalDate, zone: ZoneId): ProjectionResult {
    val recent = sessions.map { it.date.atZone(zone).toLocalDate() to it.value }
        .filter { (date, _) -> !date.isBefore(today.minusDays(WINDOW_DAYS)) }
        .sortedBy { it.first }
    if (recent.size < PROJECTION_MIN_SESSIONS) return ProjectionResult.NotEnoughData
    val first = recent.first().first
    val last = recent.last().first
    val spanTooShort = ChronoUnit.DAYS.between(first, last) < MIN_SPAN_DAYS
    val tooOld = ChronoUnit.DAYS.between(last, today) >= MAX_AGE_DAYS
    if (spanTooShort || tooOld) return ProjectionResult.NotEnoughData

    fun x(date: LocalDate) = ln(1 + ChronoUnit.DAYS.between(first, date) / TIME_SCALE_DAYS)
    val xs = recent.map { x(it.first) }
    val ys = recent.map { it.second }
    val n = recent.size
    val meanX = xs.average()
    val meanY = ys.average()
    val sxx = xs.sumOf { (it - meanX) * (it - meanX) }
    val sxy = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) }
    if (sxx <= 0) return ProjectionResult.NotEnoughData
    val slope = sxy / sxx
    val intercept = meanY - slope * meanX

    val horizon = today.plusDays(HORIZON_DAYS)
    val horizonX = x(horizon)
    val projected = intercept + slope * horizonX
    val best = ys.max()
    if (slope <= 0 || projected <= best) return ProjectionResult.NoProgress

    val residualSd = sqrt(xs.indices.sumOf { (ys[it] - intercept - slope * xs[it]).let { r -> r * r } } / (n - 2))
    val spread = SPREAD * residualSd * sqrt(1 + 1.0 / n + (horizonX - meanX) * (horizonX - meanX) / sxx)
    val halfWidth = maxOf(spread, MIN_HALF_WIDTH_KG)
    if (halfWidth > projected * MAX_RELATIVE_HALF_WIDTH) return ProjectionResult.TooIrregular

    // La borne basse ne descend pas sous le meilleur déjà atteint ; arrondis vers l'extérieur, au demi-kilo.
    val low = floor(maxOf(projected - halfWidth, best) / DISPLAY_STEP_KG) * DISPLAY_STEP_KG
    val high = ceil((projected + halfWidth) / DISPLAY_STEP_KG) * DISPLAY_STEP_KG
    return ProjectionResult.Available(Projection(horizon, low, high))
}

enum class MonthPart {
    EARLY,
    MID,
    LATE
}

// Période affichée à la place d'une date : début (1 à 10), mi (11 à 20) ou fin (21 et après) du mois.
fun monthPart(date: LocalDate): MonthPart = when (date.dayOfMonth) {
    in 1..10 -> MonthPart.EARLY
    in 11..20 -> MonthPart.MID
    else -> MonthPart.LATE
}
