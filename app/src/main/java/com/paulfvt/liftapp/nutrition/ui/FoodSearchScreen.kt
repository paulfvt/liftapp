package com.paulfvt.liftapp.nutrition.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.nutrition.domain.Food
import com.paulfvt.liftapp.nutrition.domain.SavedMeal
import com.paulfvt.liftapp.template.ui.ConfirmDeleteDialog

// « Ajouter au dîner » : recherche dans la table Ciqual et les aliments personnels, récents, favoris et repas
// enregistrés. Un appui sur un aliment ouvre sa quantité ; « + » d'un repas enregistré l'ajoute en entier.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    onBack: () -> Unit,
    onOpenFood: (Long) -> Unit,
    onCreateFood: () -> Unit,
    onDone: () -> Unit,
    viewModel: FoodSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.isDone) { if (viewModel.isDone) onDone() }
    var mealToDelete by remember { mutableStateOf<SavedMeal?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(viewModel.meal.addLabelRes())) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                }
            )
        },
        bottomBar = { SearchFooter(onCreateFood) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            SearchField(viewModel.queryText, viewModel::onQueryChange)
            FilterRow(uiState.filter, viewModel::onFilterChange)
            if (uiState.isLoading) return@Column
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.weight(1f)) {
                if (uiState.filter == FoodFilter.SAVED_MEALS) {
                    if (uiState.savedMeals.isEmpty()) item { EmptyText(R.string.nutrition_saved_meals_empty) }
                    items(uiState.savedMeals, key = { it.id }) { meal ->
                        SavedMealRow(
                            meal = meal,
                            onAdd = { viewModel.addSavedMeal(meal.id) },
                            onDelete = { mealToDelete = meal }
                        )
                    }
                } else {
                    val title = when {
                        uiState.showsRecents -> R.string.nutrition_filter_recent
                        else -> R.string.nutrition_per_100g_title
                    }
                    if (uiState.foods.isNotEmpty()) {
                        item {
                            SectionTitle(
                                stringResource(title),
                                Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                    } else {
                        item { EmptyText(emptyTextFor(uiState.filter, uiState.query.isNotBlank())) }
                    }
                    items(uiState.foods, key = { it.id }) { food -> FoodRow(food, onClick = { onOpenFood(food.id) }) }
                }
            }
        }
    }
    mealToDelete?.let { meal ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.nutrition_delete_saved_meal_title, meal.name),
            text = stringResource(R.string.nutrition_delete_saved_meal_text),
            onConfirm = {
                viewModel.deleteSavedMeal(meal.id)
                mealToDelete = null
            },
            onDismiss = { mealToDelete = null }
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    // Le clavier s'ouvre à l'arrivée sur l'écran, pas après une rotation.
    var hasFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasFocused) {
            focusRequester.requestFocus()
            hasFocused = true
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.nutrition_search_label)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.nutrition_clear_search))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .focusRequester(focusRequester)
    )
}

@Composable
private fun FilterRow(selected: FoodFilter, onSelect: (FoodFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FoodFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes())) },
                leadingIcon = if (filter == selected) {
                    { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                } else {
                    null
                }
            )
        }
    }
}

@StringRes
private fun FoodFilter.labelRes(): Int = when (this) {
    FoodFilter.ALL -> R.string.nutrition_filter_all
    FoodFilter.RECENT -> R.string.nutrition_filter_recent
    FoodFilter.FAVORITES -> R.string.nutrition_filter_favorites
    FoodFilter.SAVED_MEALS -> R.string.nutrition_filter_saved_meals
    FoodFilter.CUSTOM -> R.string.nutrition_filter_custom
}

@StringRes
private fun emptyTextFor(filter: FoodFilter, hasQuery: Boolean): Int = when {
    hasQuery -> R.string.nutrition_no_result
    filter == FoodFilter.FAVORITES -> R.string.nutrition_favorites_empty
    filter == FoodFilter.CUSTOM -> R.string.nutrition_custom_empty
    else -> R.string.nutrition_search_hint
}

@Composable
private fun EmptyText(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                nutrientsLine(food.per100g),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (food.isFavorite) {
            Icon(
                painterResource(R.drawable.ic_star),
                stringResource(R.string.nutrition_filter_favorites),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    HorizontalDivider(Modifier.padding(start = 16.dp))
}

@Composable
private fun SavedMealRow(meal: SavedMeal, onAdd: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(meal.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(
                    R.plurals.nutrition_saved_meal_summary,
                    meal.items.size,
                    meal.items.size,
                    wholeNumber(meal.total.kcal)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onAdd) {
            Icon(
                painterResource(R.drawable.ic_add),
                stringResource(R.string.nutrition_add_saved_meal, meal.name),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        OverflowMenu(
            listOf(MenuEntry(R.string.action_delete, R.drawable.ic_delete, isDestructive = true, onClick = onDelete))
        )
    }
    HorizontalDivider(Modifier.padding(start = 16.dp))
}

// Bas de l'écran : création d'un aliment et crédit de la table Ciqual (Licence Ouverte, à citer).
@Composable
private fun SearchFooter(onCreateFood: () -> Unit) {
    Column {
        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            LiftOutlinedButton(
                text = stringResource(R.string.nutrition_create_food),
                onClick = onCreateFood,
                icon = R.drawable.ic_add
            )
            Text(
                stringResource(R.string.nutrition_ciqual_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
        }
    }
}
