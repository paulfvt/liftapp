package com.paulfvt.liftapp.settings.domain

import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.WorkoutSet
import com.paulfvt.liftapp.workout.domain.exercise
import com.paulfvt.liftapp.workout.domain.set
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {
    private val paris = ZoneId.of("Europe/Paris")
    private val typeLabel: (SetType) -> String = { it.name.lowercase() }

    private fun workout(id: Long, startedAt: String, name: String, vararg exercises: WorkoutExercise) = Workout(
        id = id,
        templateId = null,
        name = name,
        startedAt = Instant.parse(startedAt),
        note = null,
        exercises = exercises.toList()
    )

    private fun item(id: Long, tracking: TrackingType, vararg sets: WorkoutSet) =
        WorkoutExercise(id, exercise(id, tracking), restSeconds = null, sets = sets.toList())

    private fun valued(values: SetValues, type: SetType = SetType.WORK, rpe: Double? = null) =
        WorkoutSet(id = 0, type = type, values = values, rpe = rpe, isCompleted = true)

    @Test
    fun `fields with a separator, a quote or a line break are quoted`() {
        val fields = listOf("a", "b;c", "dit \"non\"", "deux\nlignes")
        assertEquals("a;\"b;c\";\"dit \"\"non\"\"\";\"deux\nlignes\"", csvLine(fields))
        assertEquals("82,5;;8", csvLine(listOf("82,5", "", "8")))
    }

    @Test
    fun `a typed name that looks like a formula is neutralised`() {
        assertEquals("'=SOMME(A1)", csvText("=SOMME(A1)"))
        assertEquals("'-10 kg", csvText("-10 kg"))
        assertEquals("Push A", csvText("Push A"))
        assertEquals("", csvText(""))
    }

    @Test
    fun `the document starts with a byte order mark and uses Windows line ends`() {
        val document = csvDocument(listOf("Date", "Séance"), listOf(listOf("31/10/2026", "Push")))
        assertEquals("\uFEFFDate;Séance\r\n31/10/2026;Push\r\n", document)
    }

    @Test
    fun `history has one line per completed set in chronological order, numbered within the exercise`() {
        val later = workout(
            2,
            "2026-10-30T17:30:00Z",
            "Pull",
            item(3, TrackingType.WEIGHT_REPS, set(weightKg = 60.0, reps = 10))
        )
        val earlier = workout(
            1,
            "2026-10-28T06:05:00Z",
            "Push; lourd",
            item(
                1,
                TrackingType.WEIGHT_REPS,
                set(SetType.WARMUP, weightKg = 40.0, reps = 10),
                valued(SetValues(weightKg = 82.5, reps = 8), rpe = 8.5),
                set(weightKg = 85.0, reps = 6, completed = false)
            ),
            item(2, TrackingType.REPS, set(weightKg = -10.0, reps = 0))
        )

        val rows = historyCsvRows(listOf(later, earlier), WeightUnit.KG, paris, typeLabel)

        assertEquals(
            listOf(
                listOf("28/10/2026", "07:05", "Push; lourd", "Exercice 1", "1", "warmup", "40", "10", "", "", ""),
                listOf("28/10/2026", "07:05", "Push; lourd", "Exercice 1", "2", "work", "82,5", "8", "", "", "8,5"),
                // Exercice au poids du corps : la charge est le lest, négative pour une assistance ; 0 rep gardé.
                listOf("28/10/2026", "07:05", "Push; lourd", "Exercice 2", "1", "work", "-10", "0", "", "", ""),
                listOf("30/10/2026", "18:30", "Pull", "Exercice 3", "1", "work", "60", "10", "", "", "")
            ),
            rows
        )
    }

    @Test
    fun `loads are written in pounds when chosen, durations in seconds and distances in metres`() {
        val workout = workout(
            1,
            "2026-10-28T06:00:00Z",
            "Cardio",
            item(1, TrackingType.WEIGHT_REPS, set(weightKg = 100.0, reps = 5)),
            item(2, TrackingType.DURATION, valued(SetValues(weightKg = 20.0, durationSec = 90))),
            item(3, TrackingType.DISTANCE, valued(SetValues(durationSec = 1_500, distanceM = 5_000.0)))
        )

        val rows = historyCsvRows(listOf(workout), WeightUnit.LB, paris, typeLabel)

        assertEquals(listOf("220,5", "5", "", "", ""), rows[0].drop(6))
        // Une valeur sans rapport avec le suivi (charge sur un gainage) n'est pas exportée.
        assertEquals(listOf("", "", "90", "", ""), rows[1].drop(6))
        assertEquals(listOf("", "", "1500", "5000", ""), rows[2].drop(6))
    }

    @Test
    fun `measurements are converted to the chosen units and keep the body fat method`() {
        val labels = MeasurementCsvLabels(
            type = { it.name },
            unit = {
                when (it) {
                    MeasurementType.BODY_WEIGHT -> "lb"
                    MeasurementType.BODY_FAT -> "%"
                    else -> "in"
                }
            },
            method = { it.name }
        )
        val measurements = listOf(
            BodyMeasurement(2, MeasurementType.ARM, 38.1, null, Instant.parse("2026-10-20T08:00:00Z")),
            BodyMeasurement(1, MeasurementType.BODY_WEIGHT, 80.0, null, Instant.parse("2026-10-01T23:30:00Z")),
            BodyMeasurement(
                3,
                MeasurementType.BODY_FAT,
                15.2,
                BodyFatMethod.SCALE,
                Instant.parse("2026-10-21T08:00:00Z")
            )
        )

        val rows = measurementCsvRows(measurements, Units(WeightUnit.LB, LengthUnit.IN), paris, labels)

        assertEquals(
            listOf(
                // 23 h 30 UTC, déjà le 2 octobre à Paris.
                listOf("02/10/2026", "BODY_WEIGHT", "176,37", "lb", ""),
                listOf("20/10/2026", "ARM", "15", "in", ""),
                listOf("21/10/2026", "BODY_FAT", "15,2", "%", "SCALE")
            ),
            rows
        )
    }

    @Test
    fun `an empty history gives no line`() {
        assertTrue(historyCsvRows(emptyList(), WeightUnit.KG, paris, typeLabel).isEmpty())
    }
}
