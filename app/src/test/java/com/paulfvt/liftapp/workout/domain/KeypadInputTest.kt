package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.FieldSlot.FIRST
import com.paulfvt.liftapp.workout.domain.FieldSlot.SECOND
import org.junit.Assert.assertEquals
import org.junit.Test

class KeypadInputTest {
    private val decimal = { text: String -> filterDecimalInput(text, allowNegative = true) }

    @Test
    fun `the first key replaces the selected value then the next ones are appended`() {
        val opened = KeypadText("82,5")
        val typed = opened.typed('9', ::filterRepsInput)
        assertEquals(KeypadText("9", selected = false), typed)
        assertEquals("95", typed.typed('5', ::filterRepsInput).text)
    }

    @Test
    fun `typing goes through the field filter`() {
        // Une seule virgule, deux décimales au plus.
        val text = KeypadText("82", selected = false).typed(',', decimal).typed(',', decimal)
        assertEquals("82,", text.text)
        assertEquals("82,25", text.typed('2', decimal).typed('5', decimal).typed('7', decimal).text)
        // Reps : trois chiffres au plus.
        assertEquals("999", KeypadText("999", selected = false).typed('1', ::filterRepsInput).text)
    }

    @Test
    fun `erasing clears a selected value or removes the last character`() {
        assertEquals("", KeypadText("80").erased().text)
        assertEquals("8", KeypadText("80", selected = false).erased().text)
        assertEquals("", KeypadText("", selected = false).erased().text)
    }

    @Test
    fun `next goes through each field of each set then each exercise`() {
        val fields = listOf(
            WorkoutExercise(1, exercise(1), null, listOf(set(id = 10), set(id = 11))),
            WorkoutExercise(2, exercise(2, TrackingType.DURATION), null, listOf(set(id = 20)))
        ).keypadFields()
        assertEquals(
            listOf(SetField(10, FIRST), SetField(10, SECOND), SetField(11, FIRST), SetField(11, SECOND)) +
                SetField(20, SECOND),
            fields
        )
        // Du poids aux reps : on reste dans la série, rien à valider.
        assertEquals(KeypadMove(SetField(10, SECOND), null), fields.moveAfter(SetField(10, FIRST)))
        // Des reps à la série suivante : la série quittée est à valider.
        assertEquals(KeypadMove(SetField(11, FIRST), 10), fields.moveAfter(SetField(10, SECOND)))
        assertEquals(KeypadMove(SetField(20, SECOND), 11), fields.moveAfter(SetField(11, SECOND)))
        // Dernière case de la séance : le pavé se ferme après avoir validé la série.
        assertEquals(KeypadMove(null, 20), fields.moveAfter(SetField(20, SECOND)))
    }

    @Test
    fun `next from a field that no longer exists closes the keypad without completing anything`() {
        val fields = listOf(WorkoutExercise(1, exercise(1), null, listOf(set(id = 10)))).keypadFields()
        assertEquals(KeypadMove(null, null), fields.moveAfter(SetField(99, FIRST)))
    }
}
