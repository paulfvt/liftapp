package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.TrackingType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

private const val MAX_INTEGER_DIGITS = 4
private const val MAX_DECIMALS = 2
private const val MAX_REPS_DIGITS = 3
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600

// RPE proposé : de 6 à 10 par demi-points.
val RPE_CHOICES: List<Double> = (12..20).map { it / 2.0 }

// Frappe dans un champ décimal (poids, distance) : chiffres, une seule virgule (le point en devient une),
// 2 décimales au plus, signe moins en tête si autorisé (lest négatif = assistance).
fun filterDecimalInput(input: String, allowNegative: Boolean): String {
    val result = StringBuilder()
    var hasSeparator = false
    var integerDigits = 0
    var decimals = 0
    for (char in input) {
        when {
            char == '-' && allowNegative && result.isEmpty() -> result.append(char)
            (char == ',' || char == '.') && !hasSeparator -> {
                hasSeparator = true
                result.append(',')
            }
            char.isDigit() && !hasSeparator && integerDigits < MAX_INTEGER_DIGITS -> {
                integerDigits++
                result.append(char)
            }
            char.isDigit() && hasSeparator && decimals < MAX_DECIMALS -> {
                decimals++
                result.append(char)
            }
        }
    }
    return result.toString()
}

fun parseDecimal(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

// « 82,5 », « 100 », « -10 » : virgule française, sans zéros inutiles, 2 décimales au plus.
fun formatDecimal(value: Double?): String {
    if (value == null) return ""
    return BigDecimal.valueOf(value).setScale(MAX_DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros()
        .toPlainString().replace('.', ',')
}

// Reps : chiffres seulement, 3 au maximum. 0 est accepté (série ratée).
fun filterRepsInput(input: String): String = input.filter(Char::isDigit).take(MAX_REPS_DIGITS)

fun parseReps(text: String): Int? = text.toIntOrNull()

// Durée saisie « 1:30 » (minutes:secondes) ou « 90 » (secondes).
fun filterDurationInput(input: String): String {
    val firstColon = input.indexOf(':')
    return input.filterIndexed { index, char -> char.isDigit() || (char == ':' && index == firstColon) }.take(8)
}

fun parseDuration(text: String): Int? {
    if (':' !in text) return text.toIntOrNull()
    val minutes = text.substringBefore(':').ifEmpty { "0" }.toIntOrNull() ?: return null
    val seconds = text.substringAfter(':').toIntOrNull() ?: return null
    return if (seconds < SECONDS_PER_MINUTE) minutes * SECONDS_PER_MINUTE + seconds else null
}

// « 0:45 », « 1:30 », « 1:05:00 » au-delà d'une heure.
fun formatDuration(seconds: Int?): String {
    if (seconds == null) return ""
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val secs = seconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, secs)
    }
}

// Distance en kilomètres pour le cardio (course, vélo), en mètres sinon (traîneau, marche du fermier).
enum class DistanceUnit(val meters: Double) {
    KILOMETER(1000.0),
    METER(1.0)
}

fun distanceUnitFor(category: ExerciseCategory): DistanceUnit =
    if (category == ExerciseCategory.CARDIO) DistanceUnit.KILOMETER else DistanceUnit.METER

fun parseDistance(text: String, unit: DistanceUnit): Double? = parseDecimal(text)?.times(unit.meters)

fun formatDistance(meters: Double?, unit: DistanceUnit): String = formatDecimal(meters?.div(unit.meters))

// Une série ne peut être validée que si la valeur principale de son suivi est saisie.
fun canComplete(values: SetValues, trackingType: TrackingType): Boolean = when (trackingType) {
    TrackingType.WEIGHT_REPS, TrackingType.REPS -> values.reps != null
    TrackingType.DURATION -> values.durationSec != null
    TrackingType.DISTANCE -> values.distanceM != null || values.durationSec != null
}
