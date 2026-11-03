package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.parseDecimal

// Aliment créé par l'utilisateur, tel que saisi (valeurs pour 100 g, virgule française acceptée).
data class FoodDraft(
    val name: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = ""
)

enum class FoodDraftError {
    NAME_MISSING,
    KCAL_INVALID,
    PROTEIN_INVALID,
    CARBS_INVALID,
    FAT_INVALID,
    MACROS_OVER_100G
}

const val FOOD_NAME_MAX_LENGTH = 60
private const val MAX_KCAL_PER_100G = 900.0
private const val GRAMS_PER_100G = 100.0

fun FoodDraft.errors(): Set<FoodDraftError> = buildSet {
    if (name.isBlank()) add(FoodDraftError.NAME_MISSING)
    if (parseDecimal(kcal)?.takeIf { it in 0.0..MAX_KCAL_PER_100G } == null) add(FoodDraftError.KCAL_INVALID)
    val macros = listOf(protein, carbs, fat).map { text -> parseDecimal(text)?.takeIf { it in 0.0..GRAMS_PER_100G } }
    if (macros[0] == null) add(FoodDraftError.PROTEIN_INVALID)
    if (macros[1] == null) add(FoodDraftError.CARBS_INVALID)
    if (macros[2] == null) add(FoodDraftError.FAT_INVALID)
    // 100 g d'aliment ne peuvent pas contenir plus de 100 g de protéines, glucides et lipides réunis.
    val valid = macros.filterNotNull()
    if (valid.size == macros.size && valid.sum() > GRAMS_PER_100G) add(FoodDraftError.MACROS_OVER_100G)
}

// À n'appeler que sur un brouillon sans erreur.
fun FoodDraft.toNutrients() = Nutrients(
    kcal = parseDecimal(kcal)!!,
    protein = parseDecimal(protein)!!,
    carbs = parseDecimal(carbs)!!,
    fat = parseDecimal(fat)!!
)

fun Food.toDraft() = FoodDraft(
    name = name,
    kcal = formatDecimal(per100g.kcal),
    protein = formatDecimal(per100g.protein),
    carbs = formatDecimal(per100g.carbs),
    fat = formatDecimal(per100g.fat)
)
