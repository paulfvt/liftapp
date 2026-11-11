package com.paulfvt.liftapp.nutrition.domain

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tableaux d'étiquettes simulés : chaque ligne reconnue a sa position (x, y) sur la photo, en pixels.
class LabelReadingTest {
    private fun line(text: String, x: Int, y: Int, width: Int = 300, height: Int = 30) =
        TextLine(text, left = x, top = y, right = x + width, bottom = y + height)

    // Une ligne par rangée, tout sur la même ligne de texte.
    private fun rows(vararg texts: String) = texts.mapIndexed { index, text -> line(text, 20, 100 + index * 40, 600) }

    @Test
    fun `french label read row by row`() {
        val reading = readNutritionLabel(
            rows(
                "Valeurs nutritionnelles moyennes pour 100 g",
                "Énergie 1520 kJ / 362 kcal",
                "Matières grasses 6,5 g",
                "dont acides gras saturés 1,2 g",
                "Glucides 58 g",
                "dont sucres 1,1 g",
                "Fibres alimentaires 9 g",
                "Protéines 13 g",
                "Sel 0,02 g"
            )
        )
        assertEquals(LabelReading(kcal = 362.0, protein = 13.0, carbs = 58.0, fat = 6.5, fiber = 9.0), reading)
    }

    @Test
    fun `columns read apart are put back together and the first column is for 100 g`() {
        val reading = readNutritionLabel(
            listOf(
                line("Pour 100 g", 400, 60, 150),
                line("Par portion (30 g)", 600, 60, 200),
                line("Énergie", 20, 100, 150),
                line("1520 kJ / 362 kcal", 400, 102, 180),
                line("456 kJ / 109 kcal", 600, 101, 180),
                line("Protéines", 20, 140, 150),
                line("10,5 g", 400, 141, 80),
                line("3,2 g", 600, 139, 80),
                line("Glucides", 20, 180, 150),
                line("58 g", 400, 182, 80),
                line("17 g", 600, 180, 80),
                line("Lipides", 20, 220, 150),
                line("6,5 g", 400, 219, 80),
                line("2 g", 600, 221, 80)
            )
        )
        assertEquals(LabelReading(kcal = 362.0, protein = 10.5, carbs = 58.0, fat = 6.5), reading)
    }

    // Les mêmes lignes sur une photo penchée de `degrees` : chaque centre tourne autour de l'origine.
    private fun tilted(degrees: Double, lines: List<TextLine>): List<TextLine> {
        val radians = Math.toRadians(degrees)
        return lines.map { line ->
            val x = (line.left + line.right) / 2.0
            val y = (line.top + line.bottom) / 2.0
            val newX = x * cos(radians) - y * sin(radians)
            val newY = x * sin(radians) + y * cos(radians)
            val halfWidth = (line.right - line.left) / 2.0
            val halfHeight = (line.bottom - line.top) / 2.0
            TextLine(
                line.text,
                (newX - halfWidth).roundToInt(),
                (newY - halfHeight).roundToInt(),
                (newX + halfWidth).roundToInt(),
                (newY + halfHeight).roundToInt(),
                degrees.toFloat()
            )
        }
    }

    private val columns = listOf(
        line("Pour 100 g", 400, 60, 150),
        line("Énergie", 20, 100, 150),
        line("1520 kJ / 362 kcal", 400, 100, 180),
        line("Protéines", 20, 140, 150),
        line("10,5 g", 400, 140, 80),
        line("Glucides", 20, 180, 150),
        line("58 g", 400, 180, 80),
        line("Lipides", 20, 220, 150),
        line("6,5 g", 400, 220, 80)
    )

    @Test
    fun `a tilted photo is straightened before matching rows`() {
        // Penchée de 5° : sans redressement, le chiffre des protéines semblait sur la rangée des glucides.
        val expected = LabelReading(kcal = 362.0, protein = 10.5, carbs = 58.0, fat = 6.5)
        assertEquals(expected, readNutritionLabel(tilted(5.0, columns)))
        assertEquals(expected, readNutritionLabel(tilted(-4.0, columns)))
    }

    @Test
    fun `the ingredient list is not read as the table`() {
        val reading = readNutritionLabel(
            rows(
                "Ingrédients : flocons d'avoine 58 %, sucre, matière grasse de palme 6 g, protéines de lait, sel.",
                "protéines de lait 3 g, arôme.",
                "matière grasse (2 g), sel.",
                "Valeurs nutritionnelles pour 100 g",
                "Énergie 1520 kJ / 362 kcal",
                "Matières grasses 6,5 g",
                "Glucides 58 g",
                "Protéines 13 g"
            )
        )
        assertEquals(LabelReading(kcal = 362.0, protein = 13.0, carbs = 58.0, fat = 6.5), reading)
    }

    @Test
    fun `energy units announced by the label are used for bare numbers`() {
        assertEquals(362.0, readNutritionLabel(rows("pour 100 g", "Énergie (kJ/kcal) 1520/362")).kcal!!, 0.0)
        assertEquals(362.0, readNutritionLabel(rows("pour 100 g", "Valeur énergétique (kcal) 362")).kcal!!, 0.0)
        assertEquals(363.0, readNutritionLabel(rows("pour 100 g", "Énergie (kJ) 1520")).kcal!!, 0.0)
    }

    @Test
    fun `a sub-row stuck to its row is cut off`() {
        val reading = readNutritionLabel(
            rows(
                "pour 100 g",
                "Glucides 58 g dont sucres 5 g",
                "Matières grasses 6,5 g dont acides gras saturés 1,2 g",
                "dont matières grasses saturées 1,2 g"
            )
        )
        assertEquals(58.0, reading.carbs!!, 0.0)
        assertEquals(6.5, reading.fat!!, 0.0)
    }

    @Test
    fun `common misreadings are fixed`() {
        val reading = readNutritionLabel(
            rows("pour 100 g", "Énergie 1520 kJ / 362 kcai", "Protéines 139", "Lipides O,5 g", "Glucides l8 g")
        )
        // « kcai » pour kcal, « 13g » lu « 139 » (plus de 100 g est impossible), O pour zéro, l pour un 1.
        assertEquals(LabelReading(kcal = 362.0, protein = 13.0, carbs = 18.0, fat = 0.5), reading)
    }

    @Test
    fun `more than 900 kcal are kJ read with the wrong unit`() {
        assertEquals(363.0, readNutritionLabel(rows("pour 100 g", "Énergie 1520 kcal")).kcal!!, 0.0)
    }

    @Test
    fun `fibres are read to check the calories but not counted as a value`() {
        val reading = readNutritionLabel(rows("pour 100 g", "Fibres alimentaires 9 g"))
        assertEquals(9.0, reading.fiber!!, 0.0)
        assertEquals(0, reading.foundCount)
    }

    @Test
    fun `a misread 100 g header still counts as per 100 g`() {
        val reading = readNutritionLabel(rows("Pour 1009", "Par portion (30 g)", "Protéines 13 g 3,9 g"))
        assertFalse(reading.perPortionOnly)
        assertEquals(13.0, reading.protein!!, 0.0)
    }

    @Test
    fun `kcal on the row below kJ are found`() {
        val reading = readNutritionLabel(
            listOf(
                line("pour 100 g", 400, 60, 150),
                line("Energie", 20, 100, 150),
                line("1520 kJ", 400, 100, 120),
                line("362 kcal", 400, 132, 120),
                line("Protéines", 20, 170, 150),
                line("13 g", 400, 170, 80)
            )
        )
        assertEquals(362.0, reading.kcal!!, 0.0)
        assertEquals(13.0, reading.protein!!, 0.0)
    }

    @Test
    fun `kJ alone are converted and thousands may have a space`() {
        assertEquals(363.0, readNutritionLabel(rows("pour 100 g", "Énergie 1 520 kJ")).kcal!!, 0.0)
        assertEquals(362.0, readNutritionLabel(rows("pour 100 g", "Énergie 1 520 kJ 362 kcal")).kcal!!, 0.0)
    }

    @Test
    fun `english label with units stuck to numbers`() {
        val reading = readNutritionLabel(
            rows(
                "Nutrition per 100g",
                "Energy 1520kJ/362kcal",
                "Fat 6.5g",
                "of which saturates 1.2g",
                "Carbohydrate 58g",
                "of which sugars 1.1g",
                "Protein 13g"
            )
        )
        assertEquals(LabelReading(kcal = 362.0, protein = 13.0, carbs = 58.0, fat = 6.5), reading)
    }

    @Test
    fun `values below the detection limit count for half`() {
        val reading =
            readNutritionLabel(rows("pour 100 ml", "Énergie 42 kcal", "Matières grasses < 0,5 g", "Protéines 0 g"))
        assertEquals(0.25, reading.fat!!, 0.0)
        assertEquals(0.0, reading.protein!!, 0.0)
    }

    @Test
    fun `percentages and milligrams are not taken for grams`() {
        val reading = readNutritionLabel(rows("pour 100 g", "Protéines 18 % 12 g", "Lipides 300 mg 0,3 g"))
        assertEquals(12.0, reading.protein!!, 0.0)
        assertEquals(0.3, reading.fat!!, 0.0)
    }

    @Test
    fun `a label given per portion only is not converted`() {
        val reading = readNutritionLabel(rows("Par portion (30 g)", "Énergie 109 kcal", "Protéines 3,2 g"))
        assertTrue(reading.perPortionOnly)
        assertEquals(0, reading.foundCount)
    }

    @Test
    fun `missing values stay unknown and noise is ignored`() {
        val reading =
            readNutritionLabel(rows("Ingrédients : farine de blé, sucre, sel.", "pour 100 g", "Protéines 9 g"))
        assertEquals(1, reading.foundCount)
        assertNull(reading.kcal)
        assertNull(reading.fat)
    }

    @Test
    fun `reading fills the draft fields but keeps the typed name`() {
        val draft = FoodDraft(name = "Muesli", kcal = "", protein = "", carbs = "", fat = "5")
        val filled = LabelReading(kcal = 362.4, protein = 10.5, carbs = 58.0, fat = null).applyTo(draft)
        assertEquals(FoodDraft(name = "Muesli", kcal = "362", protein = "10,5", carbs = "58", fat = "5"), filled)
    }
}
