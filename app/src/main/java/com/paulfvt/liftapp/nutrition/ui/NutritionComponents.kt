package com.paulfvt.liftapp.nutrition.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.drawSignalRing
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.model.Meal
import com.paulfvt.liftapp.core.ui.dayTitle
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.nutrition.domain.Nutrients
import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Textes et petits composants partagés par les écrans de la nutrition.

@StringRes
fun Meal.labelRes(): Int = when (this) {
    Meal.BREAKFAST -> R.string.meal_breakfast
    Meal.LUNCH -> R.string.meal_lunch
    Meal.DINNER -> R.string.meal_dinner
    Meal.SNACKS -> R.string.meal_snacks
}

// « Ajouter au déjeuner » : titre de la recherche et de la quantité, description du bouton + d'un repas.
@StringRes
fun Meal.addLabelRes(): Int = when (this) {
    Meal.BREAKFAST -> R.string.nutrition_add_to_breakfast
    Meal.LUNCH -> R.string.nutrition_add_to_lunch
    Meal.DINNER -> R.string.nutrition_add_to_dinner
    Meal.SNACKS -> R.string.nutrition_add_to_snacks
}

fun mealFromName(name: String): Meal = Meal.entries.firstOrNull { it.name == name } ?: Meal.SNACKS

// Quantité ou valeur pour 100 g : « 180 », « 12,5 » (une décimale au plus).
fun decimalText(value: Double): String = formatDecimal((value * 10).roundToInt() / 10.0)

@Composable
fun kcalText(kcal: Double): String = stringResource(R.string.nutrition_kcal, wholeNumber(kcal))

@Composable
fun gramsText(grams: Double): String = stringResource(R.string.nutrition_grams, decimalText(grams))

// « 151 kcal · P 31 g · G 0 g · L 3 g », sous le nom d'un aliment.
@Composable
fun nutrientsLine(values: Nutrients): String = stringResource(
    R.string.nutrition_per_100g,
    wholeNumber(values.kcal),
    decimalText(values.protein),
    decimalText(values.carbs),
    decimalText(values.fat)
)

// « Aujourd'hui », « Hier » ou « Lundi 2 novembre ».
@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.nutrition_today)
    today.minusDays(1) -> stringResource(R.string.nutrition_yesterday)
    else -> dayTitle(date).replaceFirstChar { it.titlecase(Locale.FRENCH) }
}

// Couleurs des macros : protéines en rouge clair, glucides en bleu, lipides en rose pâle (texte toujours à côté).
@Composable
fun proteinColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun carbsColor(): Color = LocalExtendedColors.current.carbs

@Composable
fun fatColor(): Color = MaterialTheme.colorScheme.secondary

private const val FILL_DURATION_MS = 600

// Anneau des calories, dessiné comme l'anneau hebdomadaire : au centre, ce qui reste (ou le dépassement). Plus petit
// que 128 dp (carte de l'accueil), le chiffre rapetisse et la légende perd « kcal » pour tenir dans l'anneau.
@Composable
fun CalorieRing(eatenKcal: Double, goalKcal: Int, modifier: Modifier = Modifier, size: Dp = 128.dp) {
    val isCompact = size < 128.dp
    val fraction by animateFloatAsState(
        targetValue = if (goalKcal > 0) (eatenKcal / goalKcal).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(FILL_DURATION_MS),
        label = "calorieRing"
    )
    val remaining = goalKcal - eatenKcal.roundToInt()
    val centerLabel = stringResource(if (remaining >= 0) R.string.nutrition_kcal_left else R.string.nutrition_kcal_over)
    val description = stringResource(
        R.string.nutrition_ring_description,
        wholeNumber(eatenKcal),
        wholeNumber(goalKcal.toDouble())
    ) + ", " + wholeNumber(abs(remaining).toDouble()) + " " + centerLabel
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val bar = MaterialTheme.colorScheme.primaryContainer
    val numberStyle = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge
    val isOver = remaining < 0
    val shortLabel = stringResource(if (isOver) R.string.nutrition_over_short else R.string.nutrition_left_short)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clearAndSetSemantics { contentDescription = description }
    ) {
        Canvas(Modifier.size(size)) { drawSignalRing(fraction, track, bar) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(wholeNumber(abs(remaining).toDouble()), style = numberStyle)
            Text(
                (if (isCompact) shortLabel else centerLabel).uppercase(Locale.FRENCH),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Barre d'une macro : « PROTÉINES 107 / 153 g » au-dessus d'une jauge. Sans objectif, la quantité seule.
@Composable
fun MacroBar(label: String, grams: Double, goalGrams: Int?, color: Color, modifier: Modifier = Modifier) {
    val value = if (goalGrams != null) {
        stringResource(R.string.nutrition_macro_of_goal, wholeNumber(grams), wholeNumber(goalGrams.toDouble()))
    } else {
        stringResource(R.string.nutrition_grams, wholeNumber(grams))
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(Locale.FRENCH),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
        if (goalGrams != null) {
            val fraction = if (goalGrams > 0) (grams / goalGrams).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(50)).background(color))
            }
        }
    }
}
