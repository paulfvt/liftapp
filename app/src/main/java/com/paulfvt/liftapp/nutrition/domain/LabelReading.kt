package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Lecture d'un tableau nutritionnel photographié (décision du 5 novembre 2026). La reconnaissance de texte rend des
// lignes et leur position sur la photo ; un tableau est souvent lu en colonnes (les libellés d'un côté, les chiffres de
// l'autre), d'où l'association par rangée : un libellé prend les chiffres alignés avec lui, à sa droite.
// Une photo est rarement droite : les positions sont d'abord redressées selon l'inclinaison des lignes, sinon le chiffre
// au bout d'une rangée large semblait appartenir à la rangée du dessous (11 novembre 2026).

// Ligne de texte reconnue : son rectangle sur la photo, tel qu'il serait sans inclinaison (même centre), et son
// inclinaison en degrés, positive quand la ligne descend vers la droite. Sans inclinaison, c'est le rectangle de la photo.
data class TextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val angle: Float = 0f
)

// Valeurs lues pour 100 g ; null quand l'app ne les a pas trouvées. Les fibres ne servent qu'à vérifier les calories
// (2 kcal par gramme), l'app ne les suit pas. perPortionOnly : l'étiquette ne donne que des valeurs par portion,
// impossibles à ramener à 100 g.
data class LabelReading(
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
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

private enum class Row { ENERGY, PROTEIN, CARBS, FAT, FIBER }

// Mots qui annoncent chaque valeur, simplifiés (sans accents ni majuscules), en français et en anglais.
private val KEYWORDS = mapOf(
    Row.ENERGY to listOf("valeur energetique", "energie", "energy"),
    Row.PROTEIN to listOf("proteines", "proteine", "protein"),
    Row.CARBS to listOf("glucides", "hydrates de carbone", "carbohydrate"),
    Row.FAT to listOf("matieres grasses", "matiere grasse", "lipides", "graisses", "fat"),
    Row.FIBER to listOf("fibres alimentaires", "fibre alimentaire", "fibres", "fibre", "fiber")
)

// Ce qui peut précéder le libellé d'une rangée. Ailleurs, le mot-clé est dans une phrase (liste des ingrédients :
// « matière grasse de palme », « protéines de lait ») ou dans une sous-rangée (« acides gras saturés »).
private val ROW_PREFIXES = setOf("", "total", "teneur en", "teneurs en")

// Sous-rangées (« dont sucres », « dont acides gras saturés ») : jamais la valeur principale. Quand la lecture colle
// une sous-rangée à sa rangée (« Glucides 58 g dont sucres 5 g »), la ligne est coupée au premier de ces mots.
private val SUB_ROW_WORDS = listOf("dont", "of which", "satur", "sucres", "sugars", "polyols", "amidon", "starch")

// Mot-clé suivi d'un complément : c'est une phrase (« protéines de lait », « matière grasse de palme »).
private val PHRASE_START = Regex("""^\s*(de|du|des|d'|of|from)\b""")

// Une rangée du tableau n'a que quelques mots ; une ligne plus bavarde est une phrase (ingrédients, conseils).
private const val MAX_ROW_WORDS = 7

private const val KJ_PER_KCAL = 4.184
private const val MAX_KCAL_PER_100G = 900.0
private const val MAX_GRAMS_PER_100G = 100.0

// Lignes assez longues pour donner une inclinaison fiable.
private const val MIN_CHARS_FOR_ANGLE = 4

// Nombre éventuellement précédé de « < » et suivi de son unité : « 362 kcal », « 10,5g », « < 0,5 g », « 1 520 kJ ».
private val NUMBER = Regex("""(<\s*)?(\d{1,3}(?: \d{3})+|\d+)(?:[.,](\d+))?\s*(kcal|kj|mg|g|%)?""")

// « pour 100 g », « per 100g », « /100 ml », ou « 100 g » seul.
private val HUNDRED = Regex("""(pour|per|par|/)\s*100|100\s*(g|ml)\b""")
private val PORTION_WORDS = listOf("portion", "serving", "par part")

private data class Token(val value: Double, val unit: String?, val integer: String, val decimals: String)

// Ligne redressée : centre et taille dans le repère de la photo remise droite.
private class Placed(
    val text: String,
    val centerX: Double,
    val centerY: Double,
    val width: Double,
    val height: Double
) {
    val left: Double get() = centerX - width / 2
    val right: Double get() = centerX + width / 2
    val top: Double get() = centerY - height / 2
    val bottom: Double get() = centerY + height / 2
}

// Libellé reconnu d'une rangée : sa valeur, la suite de la ligne où chercher les chiffres, et les unités annoncées par
// le libellé (« Énergie (kJ/kcal) »), dans l'ordre où les chiffres sans unité les suivent.
private class RowLabel(val row: Row, val rest: String, val unitHints: List<String>)

fun readNutritionLabel(lines: List<TextLine>): LabelReading {
    val placed = place(lines)
    val fullText = placed.joinToString(" ") { it.text }
    val mentionsPortion = PORTION_WORDS.any { it in fullText }
    if (mentionsPortion && !HUNDRED.containsMatchIn(fullText)) return LabelReading(perPortionOnly = true)

    val values = mutableMapOf<Row, Double>()
    for (line in placed) {
        val label = rowLabel(line.text) ?: continue
        if (label.row in values) continue
        val tokens = rowTokens(line, label.rest, placed)
        val value = if (label.row == Row.ENERGY) {
            // Les kcal de la rangée, sinon celles de la rangée du dessous (kJ et kcal sur deux rangées), sinon les kJ.
            val below = belowTokens(line, placed)
            kcalIn(tokens, label.unitHints) ?: kcalIn(below, label.unitHints)
                ?: kjAsKcalIn(tokens, label.unitHints) ?: kjAsKcalIn(below, label.unitHints)
        } else {
            tokens.firstOrNull { it.unit == null || it.unit == "g" }?.let(::grams)
        }
        value?.let { values[label.row] = it }
    }
    return LabelReading(
        kcal = values[Row.ENERGY],
        protein = values[Row.PROTEIN],
        carbs = values[Row.CARBS],
        fat = values[Row.FAT],
        fiber = values[Row.FIBER]
    )
}

// Redresse la photo : chaque centre tourne autour de l'origine de l'inclinaison médiane des lignes, en sens inverse.
private fun place(lines: List<TextLine>): List<Placed> {
    val angles = lines.filter { it.text.length >= MIN_CHARS_FOR_ANGLE }.map { it.angle.toDouble() }.sorted()
    val skew = if (angles.isEmpty()) 0.0 else angles[angles.size / 2]
    val radians = Math.toRadians(-skew)
    val cos = cos(radians)
    val sin = sin(radians)
    return lines.map { line ->
        val x = (line.left + line.right) / 2.0
        val y = (line.top + line.bottom) / 2.0
        Placed(
            text = simplify(line.text),
            centerX = x * cos - y * sin,
            centerY = x * sin + y * cos,
            width = (line.right - line.left).toDouble(),
            height = (line.bottom - line.top).toDouble()
        )
    }
}

// Sans accents ni majuscules, « œ » en « oe », espaces regroupés, puis les confusions courantes de la lecture :
// « kcai » pour « kcal », la lettre O pour un zéro, l ou i pour un 1 collé à un chiffre.
private fun simplify(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace("œ", "oe")
    .replace(Regex("[«‹]"), "<")
    .replace(Regex("""k\s?ca[il1|]"""), "kcal")
    .replace(Regex("""\bk\s+j\b"""), "kj")
    .replace(Regex("""(?<=[\d,.])o|(?<![a-z])o(?=[,.]?\d)"""), "0")
    .replace(Regex("""(?<=\d)[li](?=\d)|(?<![a-z])[li](?=[,.]?\d)"""), "1")
    .replace(Regex("\\s+"), " ")
    .trim()

// Libellé de rangée en tête de ligne, ou null : mot-clé absent, précédé d'autre chose qu'un préfixe connu (phrase,
// sous-rangée), ou ligne trop bavarde pour être une rangée du tableau.
private fun rowLabel(text: String): RowLabel? {
    if (Regex("[a-z]{2,}").findAll(text).count() > MAX_ROW_WORDS) return null
    val (row, range) = KEYWORDS
        .flatMap { (row, words) -> words.mapNotNull { word -> keywordRegex(word).find(text)?.let { row to it.range } } }
        .minByOrNull { (_, range) -> range.first }
        ?: return null
    val prefix = text.substring(0, range.first).replace(Regex("[^a-z ]"), " ").trim().replace(Regex(" +"), " ")
    if (prefix !in ROW_PREFIXES) return null
    // Une rangée ne met pas ses chiffres entre parenthèses ; une liste d'ingrédients si (« protéines de lait (3 g) »).
    val after = text.substring(range.last + 1).replace(Regex("""\([^)]*\d[^)]*\)"""), " ")
    if (PHRASE_START.containsMatchIn(after)) return null
    val cut = SUB_ROW_WORDS.mapNotNull { word -> after.indexOf(word).takeIf { it >= 0 } }.minOrNull()
    val rest = if (cut == null) after else after.substring(0, cut)
    val labelPart = text.substring(0, range.last + 1) + rest.takeWhile { !it.isDigit() }
    val unitHints = Regex("kcal|kj").findAll(labelPart).map { it.value }.toList()
    return RowLabel(row, rest, unitHints)
}

private fun keywordRegex(word: String) = Regex("""\b${Regex.escape(word)}""")

// Chiffres de la rangée : ceux de la ligne du libellé (après le mot-clé), puis ceux des lignes alignées à sa droite,
// de gauche à droite. La première colonne d'un tableau européen est celle pour 100 g.
private fun rowTokens(label: Placed, rest: String, all: List<Placed>): List<Token> {
    val sameRow = all.filter { other -> other !== label && other.left > label.left && isSameRow(label, other) }
        .sortedBy { it.left }
    return tokens(rest) + sameRow.flatMap { tokens(it.text) }
}

private fun isSameRow(a: Placed, b: Placed): Boolean = abs(a.centerY - b.centerY) * 2 < maxOf(a.height, b.height)

// Lignes juste sous la rangée de l'énergie, pour les tableaux qui écrivent les kJ et les kcal sur deux rangées.
private fun belowTokens(label: Placed, all: List<Placed>): List<Token> {
    val maxGap = label.height * 2
    return all.filter { other ->
        other.top >= label.bottom - label.height / 2 && other.top - label.bottom <= maxGap && other.right > label.left
    }.sortedWith(compareBy({ it.top }, { it.left }))
        .flatMap { tokens(it.text) }
}

private fun tokens(text: String): List<Token> = NUMBER.findAll(text).mapNotNull { match ->
    val unit = match.groupValues[4].ifEmpty { null }
    if (unit == "%") return@mapNotNull null
    val integer = match.groupValues[2].replace(" ", "")
    val decimals = match.groupValues[3]
    val value = (if (decimals.isEmpty()) integer else "$integer.$decimals").toDouble()
    // « < 0,5 g » : sous la limite de détection, compté pour la moitié (même règle que la table Ciqual).
    Token(if (match.groupValues[1].isNotEmpty()) value / 2 else value, unit, integer, decimals)
}.toList()

// Grammes pour 100 g. Plus de 100 g est impossible : un « 9 » final sans unité est presque toujours un « g » mal lu
// (« 13g » lu « 139 »), on le retire ; sinon la valeur est écartée.
private fun grams(token: Token): Double? = when {
    token.value <= MAX_GRAMS_PER_100G -> token.value
    token.unit == null && token.decimals.isEmpty() && token.integer.length > 1 && token.integer.endsWith("9") ->
        token.integer.dropLast(1).toDouble().takeIf { it <= MAX_GRAMS_PER_100G }
    else -> null
}

// Énergie en kcal : un chiffre suivi de « kcal », sinon un chiffre sans unité que le libellé annonce en kcal
// (« Énergie (kJ/kcal) 1520/362 »). Plus de 900 kcal pour 100 g est impossible : ce sont des kJ mal étiquetés.
private fun kcalIn(tokens: List<Token>, unitHints: List<String>): Double? =
    tokens.firstOrNull { it.unit == "kcal" && it.value <= MAX_KCAL_PER_100G }?.value
        ?: hinted(tokens, unitHints, "kcal")?.takeIf { it <= MAX_KCAL_PER_100G }

// Des kJ convertis en kcal, à défaut de kcal.
private fun kjAsKcalIn(tokens: List<Token>, unitHints: List<String>): Double? {
    val kj = tokens.firstOrNull { it.unit == "kj" }?.value
        ?: hinted(tokens, unitHints, "kj")
        ?: tokens.firstOrNull { it.unit == "kcal" }?.value
    return kj?.let { (it / KJ_PER_KCAL).roundToInt().toDouble() }?.takeIf { it <= MAX_KCAL_PER_100G }
}

// Chiffres sans unité pris dans l'ordre des unités annoncées par le libellé.
private fun hinted(tokens: List<Token>, unitHints: List<String>, unit: String): Double? =
    unitHints.zip(tokens.filter { it.unit == null }).firstOrNull { it.first == unit }?.second?.value
