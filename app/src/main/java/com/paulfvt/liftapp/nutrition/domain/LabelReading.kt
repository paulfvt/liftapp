package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Lecture d'un tableau nutritionnel photographié (décision du 5 novembre 2026). La reconnaissance de texte rend des
// lignes et leur position sur la photo ; un tableau est souvent lu en colonnes (les libellés d'un côté, les chiffres de
// l'autre), d'où l'association par rangée : un libellé prend les chiffres alignés avec lui, à sa droite.

// Ligne de texte reconnue, avec sa boîte en pixels de l'image.
data class TextLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = bottom - top
}

// Valeurs lues pour 100 g ; null quand l'app ne les a pas trouvées. perPortionOnly : l'étiquette ne donne que des
// valeurs par portion, impossibles à ramener à 100 g.
data class LabelReading(
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val perPortionOnly: Boolean = false
) {
    val foundCount: Int get() = listOfNotNull(kcal, protein, carbs, fat).size

    // Remplit les champs trouvés d'un aliment en cours de saisie ; le nom et les champs non lus restent tels quels.
    fun applyTo(draft: FoodDraft) = draft.copy(
        kcal = kcal?.let { formatDecimal(it.roundToInt().toDouble()) } ?: draft.kcal,
        protein = protein?.let(::gramsText) ?: draft.protein,
        carbs = carbs?.let(::gramsText) ?: draft.carbs,
        fat = fat?.let(::gramsText) ?: draft.fat
    )

    private fun gramsText(value: Double) = formatDecimal((value * 100).roundToInt() / 100.0)
}

private enum class LabelField { ENERGY, PROTEIN, CARBS, FAT }

// Mots qui annoncent chaque valeur, simplifiés (sans accents ni majuscules), en français et en anglais.
private val KEYWORDS = mapOf(
    LabelField.ENERGY to listOf("valeur energetique", "energie", "energy"),
    LabelField.PROTEIN to listOf("proteines", "proteine", "protein"),
    LabelField.CARBS to listOf("glucides", "hydrates de carbone", "carbohydrate"),
    LabelField.FAT to listOf("matieres grasses", "matiere grasse", "lipides", "graisses", "fat")
)

// Sous-lignes (« dont sucres », « dont acides gras saturés ») : jamais la valeur principale.
private val SUB_ROW_WORDS = listOf("dont", "of which", "satur", "sucres", "sugars", "polyols", "amidon", "starch")

private const val KJ_PER_KCAL = 4.184

// Nombre éventuellement précédé de « < » et suivi de son unité : « 362 kcal », « 10,5g », « < 0,5 g », « 1 520 kJ ».
private val NUMBER = Regex("""(<\s*)?(\d{1,3}(?: \d{3})+|\d+)(?:[.,](\d+))?\s*(kcal|kj|mg|g|%)?""")

private data class Token(val value: Double, val unit: String?)

fun readNutritionLabel(lines: List<TextLine>): LabelReading {
    val all = lines.map { it to simplify(it.text) }
    val fullText = all.joinToString(" ") { it.second }
    val mentionsHundred = Regex("""100\s*(g|ml)\b""").containsMatchIn(fullText)
    val mentionsPortion = listOf("portion", "serving", "par part").any { it in fullText }
    if (mentionsPortion && !mentionsHundred) return LabelReading(perPortionOnly = true)

    val values = mutableMapOf<LabelField, Double>()
    for ((line, text) in all) {
        if (SUB_ROW_WORDS.any { it in text }) continue
        val (field, keywordEnd) = matchKeyword(text) ?: continue
        if (field in values) continue
        val tokens = rowTokens(line, text.substring(keywordEnd), all)
        val value = if (field == LabelField.ENERGY) {
            // Les kcal de la rangée, sinon celles de la rangée du dessous (kJ et kcal sur deux rangées), sinon les kJ.
            val below = belowTokens(line, all)
            kcalIn(tokens) ?: kcalIn(below) ?: kjAsKcal(tokens) ?: kjAsKcal(below)
        } else {
            tokens.firstOrNull { it.unit == null || it.unit == "g" }?.value
        }
        value?.let { values[field] = it }
    }
    return LabelReading(
        kcal = values[LabelField.ENERGY],
        protein = values[LabelField.PROTEIN],
        carbs = values[LabelField.CARBS],
        fat = values[LabelField.FAT]
    )
}

// Sans accents ni majuscules, « œ » en « oe », espaces regroupés ; chiffres, virgules et « < » gardés.
private fun simplify(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace("œ", "oe")
    .replace(Regex("\\s+"), " ")
    .trim()

// Mot-clé le plus à gauche de la ligne (au début d'un mot), et la position de sa fin.
private fun matchKeyword(text: String): Pair<LabelField, Int>? = KEYWORDS
    .flatMap { (field, words) -> words.mapNotNull { word -> keywordRegex(word).find(text)?.let { field to it.range } } }
    .minByOrNull { (_, range) -> range.first }
    ?.let { (field, range) -> field to range.last + 1 }

private fun keywordRegex(word: String) = Regex("""\b${Regex.escape(word)}""")

// Chiffres de la rangée : ceux de la ligne du libellé (après le mot-clé), puis ceux des lignes alignées à sa droite,
// de gauche à droite. La première colonne d'un tableau européen est celle pour 100 g.
private fun rowTokens(label: TextLine, rest: String, all: List<Pair<TextLine, String>>): List<Token> {
    val sameRow = all.filter { (other, _) ->
        other !== label && other.left > label.left && isSameRow(label, other)
    }.sortedBy { it.first.left }
    return tokens(rest) + sameRow.flatMap { tokens(it.second) }
}

private fun isSameRow(a: TextLine, b: TextLine): Boolean = abs(a.centerY - b.centerY) * 2 < maxOf(a.height, b.height)

// Lignes juste sous la rangée de l'énergie, pour les tableaux qui écrivent les kJ et les kcal sur deux rangées.
private fun belowTokens(label: TextLine, all: List<Pair<TextLine, String>>): List<Token> {
    val maxGap = label.height * 2
    return all.filter { (other, _) ->
        other.top >= label.bottom - label.height / 2 && other.top - label.bottom <= maxGap && other.right > label.left
    }.sortedWith(compareBy({ it.first.top }, { it.first.left }))
        .flatMap { tokens(it.second) }
}

private fun tokens(text: String): List<Token> = NUMBER.findAll(text).mapNotNull { match ->
    val unit = match.groupValues[4].ifEmpty { null }
    if (unit == "%") return@mapNotNull null
    val integer = match.groupValues[2].replace(" ", "")
    val decimals = match.groupValues[3]
    val value = (if (decimals.isEmpty()) integer else "$integer.$decimals").toDouble()
    // « < 0,5 g » : sous la limite de détection, compté pour la moitié (même règle que la table Ciqual).
    Token(if (match.groupValues[1].isNotEmpty()) value / 2 else value, unit)
}.toList()

private fun kcalIn(tokens: List<Token>): Double? = tokens.firstOrNull { it.unit == "kcal" }?.value

private fun kjAsKcal(tokens: List<Token>): Double? =
    tokens.firstOrNull { it.unit == "kj" }?.value?.let { (it / KJ_PER_KCAL).roundToInt().toDouble() }
