package com.paulfvt.liftapp.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.nutrition.ui.CalorieRing
import com.paulfvt.liftapp.nutrition.ui.MacroBar
import com.paulfvt.liftapp.nutrition.ui.carbsColor
import com.paulfvt.liftapp.nutrition.ui.fatColor
import com.paulfvt.liftapp.nutrition.ui.proteinColor
import java.util.Locale

private val RING_SIZE = 96.dp

// Carte « Nutrition du jour » de l'accueil (étape 7 de la phase 12) : l'anneau des calories et les trois macros du
// journal, en plus compact. Un appui ouvre l'onglet Nutrition.
@Composable
fun NutritionTodayCard(nutrition: NutritionToday, onClick: () -> Unit) {
    val total = nutrition.total
    val goal = nutrition.goal
    LiftCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            if (goal != null) {
                CalorieRing(eatenKcal = total.kcal, goalKcal = goal.kcal, size = RING_SIZE)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(wholeNumber(total.kcal), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.nutrition_kcal_eaten).uppercase(Locale.FRENCH),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                if (goal != null) {
                    Text(
                        stringResource(
                            R.string.nutrition_eaten_of_goal,
                            wholeNumber(total.kcal),
                            wholeNumber(goal.kcal.toDouble())
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MacroBar(stringResource(R.string.nutrition_protein), total.protein, goal?.proteinGrams, proteinColor())
                MacroBar(stringResource(R.string.nutrition_carbs), total.carbs, goal?.carbsGrams, carbsColor())
                MacroBar(stringResource(R.string.nutrition_fat), total.fat, goal?.fatGrams, fatColor())
            }
        }
    }
}
