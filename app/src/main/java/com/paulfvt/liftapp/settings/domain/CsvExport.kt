package com.paulfvt.liftapp.settings.domain

import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.domain.inputValue
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import com.paulfvt.liftapp.profile.domain.toDisplay
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Export CSV (cahier § 4.11) au format d'Excel en français (décision du 28 octobre 2026) : point-virgule entre les
// colonnes, virgule décimale, dates en jj/mm/aaaa. Les en-têtes, traduits, viennent des ressources.

private const val SEPARATOR = ";"

// Fin de ligne Windows, celle de la norme CSV (RFC 4180).
private const val LINE_END = "\r\n"

// Marque d'ordre des octets : sans elle, Excel lit le fichier dans l'encodage de Windows et abîme les accents.
private const val BYTE_ORDER_MARK = "\uFEFF"

private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TIME = DateTimeFormatter.ofPattern("HH:mm")

// Débuts qu'un tableur prendrait pour une formule (« =SOMME(…) ») dans un texte saisi par l'utilisateur.
private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

fun csvDocument(header: List<String>, rows: List<List<String>>): String = buildString {
    append(BYTE_ORDER_MARK)
    (listOf(header) + rows).forEach { append(csvLine(it)).append(LINE_END) }
}

// Un champ qui contient le séparateur, un guillemet ou un retour à la ligne est mis entre guillemets, ses guillemets
// doublés.
fun csvLine(fields: List<String>): String = fields.joinToString(SEPARATOR) { field ->
    if (field.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + field.replace("\"", "\"\"") + "\""
    } else {
        field
    }
}

// Nom saisi par l'utilisateur : une apostrophe devant un début de formule la neutralise, le tableur l'affiche telle
// quelle sans l'exécuter.
fun csvText(text: String): String = if (text.isNotEmpty() && text[0] in FORMULA_STARTS) "'$text" else text

// Historique : une ligne par série validée des séances terminées, dans l'ordre chronologique. Les colonnes suivent
// R.array : date, heure, séance, exercice, n° de série, type, charge (dans l'unité choisie), reps, durée (s),
// distance (m), RPE. Seules les valeurs qui ont un sens pour le suivi de l'exercice sont remplies.
fun historyCsvRows(
    workouts: List<Workout>,
    unit: WeightUnit,
    zone: ZoneId,
    setTypeLabel: (SetType) -> String
): List<List<String>> = workouts.sortedBy { it.startedAt }.flatMap { workout ->
    val start = workout.startedAt.atZone(zone)
    workout.exercises.flatMap { item ->
        val tracking = item.exercise.trackingType
        item.sets.filter { it.isCompleted }.mapIndexed { index, set ->
            listOf(
                DATE.format(start),
                TIME.format(start),
                csvText(workout.name),
                csvText(item.exercise.name),
                (index + 1).toString()
            ) + setTypeLabel(set.type) + set.values.csvFields(tracking, unit) + formatDecimal(set.rpe)
        }
    }
}

private fun SetValues.csvFields(tracking: TrackingType, unit: WeightUnit): List<String> {
    val withLoad = tracking == TrackingType.WEIGHT_REPS || tracking == TrackingType.REPS
    return listOf(
        weightKg?.takeIf { withLoad }?.let { formatDecimal(unit.inputValue(it)) }.orEmpty(),
        reps?.takeIf { withLoad }?.toString().orEmpty(),
        durationSec?.takeIf { !withLoad }?.toString().orEmpty(),
        formatDecimal(distanceM?.takeIf { tracking == TrackingType.DISTANCE })
    )
}

// Libellés des mesures, fournis par les ressources.
data class MeasurementCsvLabels(
    val type: (MeasurementType) -> String,
    val unit: (MeasurementType) -> String,
    val method: (BodyFatMethod) -> String
)

// Mesures : une ligne par saisie, dans l'ordre chronologique. Colonnes : date, mesure, valeur (dans l'unité
// choisie), unité, méthode (masse grasse seulement).
fun measurementCsvRows(
    measurements: List<BodyMeasurement>,
    units: Units,
    zone: ZoneId,
    labels: MeasurementCsvLabels
): List<List<String>> = measurements.sortedBy { it.measuredAt }.map { measurement ->
    listOf(
        DATE.format(measurement.measuredAt.atZone(zone)),
        labels.type(measurement.type),
        formatDecimal(measurement.type.toDisplay(measurement.value, units)),
        labels.unit(measurement.type),
        measurement.method?.let(labels.method).orEmpty()
    )
}
