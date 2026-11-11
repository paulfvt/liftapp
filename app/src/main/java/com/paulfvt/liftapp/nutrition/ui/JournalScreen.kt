package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.model.Meal
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.nutrition.domain.JournalEntry
import com.paulfvt.liftapp.nutrition.domain.MealJournal
import com.paulfvt.liftapp.nutrition.domain.Nutrients
import com.paulfvt.liftapp.nutrition.domain.NutritionTarget
import com.paulfvt.liftapp.template.ui.NameDialog
import java.time.LocalDate

// Onglet Nutrition : le journal d'un jour, calories et macros en haut, puis les 4 repas.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onAddFood: (date: LocalDate, meal: Meal) -> Unit,
    onOpenEntry: (JournalEntry) -> Unit,
    onOpenNeeds: () -> Unit,
    onOpenTrend: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel.message) {
        val text = when (val message = viewModel.message) {
            null -> return@LaunchedEffect
            is JournalMessage.Copied -> resources.getQuantityString(
                R.plurals.nutrition_copied,
                message.count,
                message.count
            )
            JournalMessage.NothingToCopy -> resources.getString(R.string.nutrition_nothing_to_copy)
            JournalMessage.MealSaved -> resources.getString(R.string.nutrition_meal_saved)
        }
        // Effacé une fois affiché : l'effacer avant relancerait cet effet et couperait le message aussitôt.
        snackbar.showSnackbar(text)
        viewModel.onMessageShown()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.nav_nutrition)) },
                actions = {
                    IconButton(onClick = onOpenTrend) {
                        Icon(painterResource(R.drawable.ic_trending_up), stringResource(R.string.nutrition_trend_title))
                    }
                    IconButton(onClick = onOpenNeeds) {
                        Icon(painterResource(R.drawable.ic_flag), stringResource(R.string.nutrition_needs_title))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "day") {
                DayNavigator(
                    date = uiState.date,
                    today = uiState.today,
                    canGoForward = !uiState.isToday,
                    onPrevious = viewModel::showPreviousDay,
                    onNext = viewModel::showNextDay
                )
            }
            item(key = "summary") { DaySummary(uiState.total, uiState.goal, onOpenNeeds) }
            item(key = "meals-title") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    SectionTitle(stringResource(R.string.nutrition_meals), Modifier.weight(1f))
                    val copyLabel =
                        if (uiState.isToday) R.string.nutrition_copy_yesterday else R.string.nutrition_copy_previous_day
                    LiftTextButton(
                        text = stringResource(copyLabel),
                        onClick = viewModel::copyPreviousDay,
                        icon = R.drawable.ic_content_copy
                    )
                }
            }
            items(uiState.meals, key = { it.meal.name }) { meal ->
                MealCard(
                    meal = meal,
                    onAdd = { onAddFood(uiState.date, meal.meal) },
                    onOpenEntry = onOpenEntry,
                    onSave = { viewModel.askMealName(meal.meal) }
                )
            }
        }
    }
    viewModel.mealToSave?.let { meal ->
        val entries = uiState.meals.first { it.meal == meal }.entries
        NameDialog(
            title = stringResource(R.string.nutrition_save_meal_title),
            initialName = stringResource(meal.labelRes()),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { name ->
                viewModel.dismissMealName()
                viewModel.saveMeal(name, entries)
            },
            onDismiss = viewModel::dismissMealName
        )
    }
}

@Composable
private fun DayNavigator(
    date: LocalDate,
    today: LocalDate,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.nutrition_previous_day))
        }
        Text(
            dayLabel(date, today),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.nutrition_next_day))
        }
    }
}

// Carte du haut : anneau des calories et barres des macros si un objectif existe, sinon les totaux seuls.
@Composable
private fun DaySummary(total: Nutrients, goal: NutritionTarget?, onOpenNeeds: () -> Unit) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (goal != null) {
                    stringResource(
                        R.string.nutrition_eaten_of_goal,
                        wholeNumber(total.kcal),
                        wholeNumber(goal.kcal.toDouble())
                    )
                } else {
                    stringResource(R.string.nutrition_no_goal)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (goal != null) {
                    CalorieRing(eatenKcal = total.kcal, goalKcal = goal.kcal)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(wholeNumber(total.kcal), style = MaterialTheme.typography.displaySmall)
                        Text(
                            stringResource(R.string.nutrition_kcal_eaten).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                    MacroBar(
                        stringResource(R.string.nutrition_protein),
                        total.protein,
                        goal?.proteinGrams,
                        proteinColor()
                    )
                    MacroBar(stringResource(R.string.nutrition_carbs), total.carbs, goal?.carbsGrams, carbsColor())
                    MacroBar(stringResource(R.string.nutrition_fat), total.fat, goal?.fatGrams, fatColor())
                }
            }
            if (goal == null) {
                LiftOutlinedButton(
                    text = stringResource(R.string.nutrition_set_goal),
                    onClick = onOpenNeeds,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MealCard(meal: MealJournal, onAdd: () -> Unit, onOpenEntry: (JournalEntry) -> Unit, onSave: () -> Unit) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
            Text(
                stringResource(meal.meal.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Text(kcalText(meal.total.kcal), style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onAdd) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    stringResource(meal.meal.addLabelRes()),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (meal.entries.isNotEmpty()) {
                OverflowMenu(listOf(MenuEntry(R.string.nutrition_save_meal, onClick = onSave)))
            }
        }
        if (meal.entries.isEmpty()) {
            HorizontalDivider()
            Text(
                stringResource(R.string.nutrition_meal_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        meal.entries.forEach { entry ->
            HorizontalDivider()
            EntryRow(entry, onClick = { onOpenEntry(entry) })
        }
    }
}

@Composable
private fun EntryRow(entry: JournalEntry, onClick: () -> Unit) {
    val grams = gramsText(entry.grams)
    val kcal = wholeNumber(entry.nutrients.kcal)
    val description = stringResource(R.string.nutrition_entry_description, entry.foodName, grams, kcal)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                entry.foodName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(grams, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(kcal, style = MaterialTheme.typography.titleSmall)
    }
}
