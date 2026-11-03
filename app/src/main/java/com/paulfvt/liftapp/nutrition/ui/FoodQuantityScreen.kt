package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.NumericKeypad
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.designsystem.component.hairlineBorder
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.nutrition.domain.Food
import com.paulfvt.liftapp.nutrition.domain.GRAMS_STEP
import com.paulfvt.liftapp.nutrition.domain.Nutrients
import java.util.Locale
import kotlin.math.abs

// Quantité d'un aliment, saisie au pavé numérique des séances : − et + de 10 g, portions « 100 g » et « Dernière
// fois », calories et macros de la quantité, et ce qui restera dans la journée.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodQuantityScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onEditFood: (Long) -> Unit,
    viewModel: FoodQuantityViewModel = hiltViewModel()
) {
    val food by viewModel.food.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.isDone) { if (viewModel.isDone) onDone() }
    var keypadVisible by rememberSaveable { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isEditing) R.string.nutrition_edit_quantity else viewModel.meal.addLabelRes()
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = { food?.let { QuantityActions(it, viewModel, onEditFood) } }
            )
        },
        bottomBar = {
            if (keypadVisible) {
                NumericKeypad(
                    cornerKey = ',',
                    stepLabel = stringResource(R.string.nutrition_grams, decimalText(GRAMS_STEP)),
                    actionLabel = stringResource(
                        if (viewModel.isEditing) R.string.action_save else R.string.nutrition_add
                    ),
                    onType = viewModel::type,
                    onErase = viewModel::erase,
                    onStep = viewModel::step,
                    onClose = { keypadVisible = false },
                    onAction = viewModel::save,
                    actionEnabled = viewModel.isValid && food != null,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { padding ->
        val current = food ?: return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(current.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(
                    if (current.isCustom) R.string.nutrition_source_custom else R.string.nutrition_source_ciqual,
                    wholeNumber(current.per100g.kcal)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QuantityField(
                text = viewModel.input.text,
                selected = viewModel.input.selected,
                isOpen = keypadVisible,
                onClick = { keypadVisible = true }
            )
            if (!viewModel.isValid) {
                Text(
                    stringResource(R.string.nutrition_grams_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            PortionChips(viewModel.grams, viewModel.lastGrams, onChoose = viewModel::choosePortion)
            val portion = viewModel.nutrientsFor(current)
            NutrientTiles(portion)
            budget?.remainingAfter(portion.kcal)?.let { remaining ->
                Text(
                    if (remaining >= 0) {
                        stringResource(R.string.nutrition_remaining_after, wholeNumber(remaining))
                    } else {
                        stringResource(R.string.nutrition_over_after, wholeNumber(abs(remaining)))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuantityActions(food: Food, viewModel: FoodQuantityViewModel, onEditFood: (Long) -> Unit) {
    IconButton(onClick = viewModel::toggleFavorite) {
        Icon(
            painterResource(if (food.isFavorite) R.drawable.ic_star else R.drawable.ic_star_border),
            stringResource(
                if (food.isFavorite) R.string.nutrition_favorite_remove else R.string.nutrition_favorite_add
            ),
            tint = MaterialTheme.colorScheme.run { if (food.isFavorite) tertiary else onSurfaceVariant }
        )
    }
    val entries = listOfNotNull(
        MenuEntry(R.string.nutrition_edit_food, R.drawable.ic_edit) { onEditFood(food.id) }.takeIf { food.isCustom },
        MenuEntry(
            R.string.nutrition_remove_entry,
            R.drawable.ic_delete,
            isDestructive = true,
            onClick = viewModel::delete
        )
            .takeIf { viewModel.isEditing }
    )
    if (entries.isNotEmpty()) OverflowMenu(entries)
}

// Champ de la quantité, contour rouge tant que le pavé est ouvert (comme les cases des séances).
@Composable
private fun QuantityField(text: String, selected: Boolean, isOpen: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    val description =
        stringResource(R.string.nutrition_quantity) + ", " + stringResource(R.string.nutrition_grams, text)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.nutrition_quantity).uppercase(Locale.FRENCH),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(colors.surfaceContainerHighest, shape)
                .border(if (isOpen) BorderStroke(2.dp, colors.primary) else hairlineBorder(), shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.displaySmall,
                modifier = if (selected && text.isNotEmpty()) {
                    Modifier
                        .background(colors.primary.copy(alpha = SELECTION_ALPHA), RoundedCornerShape(4.dp))
                        .padding(horizontal = 2.dp)
                } else {
                    Modifier
                }
            )
            Text(
                " " + stringResource(R.string.nutrition_unit_grams),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurfaceVariant
            )
        }
    }
}

private const val SELECTION_ALPHA = 0.35f
private const val PORTION_100_G = 100.0

@Composable
private fun PortionChips(grams: Double?, lastGrams: Double?, onChoose: (Double) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = grams == PORTION_100_G,
            onClick = { onChoose(PORTION_100_G) },
            label = { Text(stringResource(R.string.nutrition_grams, decimalText(PORTION_100_G))) }
        )
        if (lastGrams != null && lastGrams != PORTION_100_G) {
            FilterChip(
                selected = grams == lastGrams,
                onClick = { onChoose(lastGrams) },
                label = { Text(stringResource(R.string.nutrition_portion_last, decimalText(lastGrams))) }
            )
        }
    }
}

@Composable
private fun NutrientTiles(values: Nutrients) {
    val grams = stringResource(R.string.nutrition_unit_grams)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NutrientTile(
            stringResource(R.string.nutrition_kcal_short),
            wholeNumber(values.kcal),
            null,
            null,
            Modifier.weight(1f)
        )
        NutrientTile(
            stringResource(R.string.nutrition_protein_short),
            wholeNumber(values.protein),
            grams,
            proteinColor(),
            Modifier.weight(1f)
        )
        NutrientTile(
            stringResource(R.string.nutrition_carbs_short),
            wholeNumber(values.carbs),
            grams,
            carbsColor(),
            Modifier.weight(1f)
        )
        NutrientTile(
            stringResource(R.string.nutrition_fat_short),
            wholeNumber(values.fat),
            grams,
            fatColor(),
            Modifier.weight(1f)
        )
    }
}

// Petite tuile : pastille de couleur et libellé, valeur en gros chiffres. Lue d'un seul tenant.
@Composable
private fun NutrientTile(label: String, value: String, unit: String?, color: Color?, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .border(hairlineBorder(), shape)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            color?.let { Spacer(Modifier.size(7.dp).background(it, RoundedCornerShape(50))) }
            Text(
                label.uppercase(Locale.FRENCH),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            unit?.let {
                Text(
                    " $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}
