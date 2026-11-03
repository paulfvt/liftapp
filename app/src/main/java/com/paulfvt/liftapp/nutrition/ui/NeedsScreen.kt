package com.paulfvt.liftapp.nutrition.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.model.ActivityLevel
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.model.NutritionObjective
import com.paulfvt.liftapp.core.ui.lengthText
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.nutrition.domain.NeedsBreakdown
import com.paulfvt.liftapp.nutrition.domain.NutritionTarget
import com.paulfvt.liftapp.nutrition.domain.isConsistent
import com.paulfvt.liftapp.profile.ui.measureText
import com.paulfvt.liftapp.profile.ui.sexLabel
import com.paulfvt.liftapp.template.ui.ChoiceDialog
import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.util.Locale

// « Mes besoins » : profil, activité et objectif, détail du calcul, répartition des macros.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onAddWeight: () -> Unit,
    viewModel: NeedsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.isSaved) { if (viewModel.isSaved) onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nutrition_needs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        val current = profile ?: return@Scaffold
        val breakdown = viewModel.breakdown(current)
        val target = viewModel.target(current)
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ProfileCard(current, onEditProfile, onAddWeight)
            ActivityField(viewModel.settings.activityLevel, viewModel::onActivityChange)
            ObjectiveField(viewModel.settings.objective, viewModel::onObjectiveChange)
            breakdown?.let { CalculationCard(it, viewModel.settings.activityLevel, viewModel.settings.objective) }
            ManualKcalField(viewModel)
            if (target != null) {
                MacroCard(
                    target = target,
                    proteinPerKg = viewModel.settings.proteinPerKg,
                    fatPerKg = viewModel.settings.fatPerKg,
                    onStepProtein = viewModel::stepProtein,
                    onStepFat = viewModel::stepFat
                )
                if (!target.isConsistent()) {
                    Text(
                        stringResource(R.string.nutrition_macros_inconsistent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                stringResource(R.string.nutrition_needs_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SignalButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = viewModel.canSave(current),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProfileCard(profile: NeedsProfile, onEditProfile: () -> Unit, onAddWeight: () -> Unit) {
    val missing = stringResource(R.string.nutrition_needs_missing)
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.nutrition_needs_profile))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileValue(
                    R.string.nutrition_needs_sex,
                    profile.sex?.let { stringResource(sexLabel(it)) } ?: missing,
                    Modifier.weight(1f)
                )
                ProfileValue(
                    R.string.nutrition_needs_age,
                    profile.age?.let { pluralStringResource(R.plurals.nutrition_age_years, it, it) } ?: missing,
                    Modifier.weight(1f)
                )
                ProfileValue(
                    R.string.nutrition_needs_height,
                    profile.heightCm?.let { lengthText(it) } ?: missing,
                    Modifier.weight(1f)
                )
                ProfileValue(
                    R.string.nutrition_needs_weight,
                    profile.weight?.let { measureText(MeasurementType.BODY_WEIGHT, it.value) } ?: missing,
                    Modifier.weight(1f)
                )
            }
            profile.weight?.let {
                Text(
                    stringResource(R.string.nutrition_needs_weight_date, shortDay(it.measuredAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (profile.weight == null) {
                Notice(R.string.nutrition_needs_missing_weight, R.string.nutrition_needs_add_weight, onAddWeight)
            } else if (profile.body == null) {
                Notice(R.string.nutrition_needs_missing_profile, R.string.nutrition_needs_edit_profile, onEditProfile)
            }
        }
    }
}

@Composable
private fun ProfileValue(@StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(label).uppercase(Locale.FRENCH),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Notice(@StringRes text: Int, @StringRes action: Int, onAction: () -> Unit) {
    Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    LiftOutlinedButton(text = stringResource(action), onClick = onAction)
}

@Composable
private fun ActivityField(selected: ActivityLevel, onSelect: (ActivityLevel) -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle(stringResource(R.string.nutrition_activity))
        LiftCard(onClick = { isOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(selected.labelRes()), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(selected.descriptionRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null)
            }
        }
    }
    if (isOpen) {
        ChoiceDialog(
            title = stringResource(R.string.nutrition_activity),
            options = ActivityLevel.entries,
            selected = selected,
            label = { stringResource(it.labelRes()) + " · " + stringResource(it.descriptionRes()) },
            onSelect = {
                onSelect(it)
                isOpen = false
            },
            onDismiss = { isOpen = false }
        )
    }
}

@Composable
private fun ObjectiveField(selected: NutritionObjective, onSelect: (NutritionObjective) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle(stringResource(R.string.nutrition_objective))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            NutritionObjective.entries.forEachIndexed { index, objective ->
                SegmentedButton(
                    selected = objective == selected,
                    onClick = { onSelect(objective) },
                    shape = SegmentedButtonDefaults.itemShape(index, NutritionObjective.entries.size)
                ) { Text(stringResource(objective.labelRes()), maxLines = 1) }
            }
        }
        Text(
            stringResource(selected.descriptionRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalculationCard(breakdown: NeedsBreakdown, activity: ActivityLevel, objective: NutritionObjective) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            CalculationRow(
                stringResource(R.string.nutrition_bmr),
                stringResource(R.string.nutrition_bmr_formula),
                kcalText(breakdown.basalMetabolism.toDouble())
            )
            HorizontalDivider()
            CalculationRow(
                stringResource(R.string.nutrition_expenditure),
                stringResource(
                    R.string.nutrition_expenditure_detail,
                    wholeNumber(breakdown.basalMetabolism.toDouble()),
                    formatDecimal(activity.factor)
                ),
                kcalText(breakdown.expenditure.toDouble())
            )
            HorizontalDivider()
            CalculationRow(
                stringResource(objective.labelRes()),
                stringResource(R.string.nutrition_offset_detail),
                signedKcal(breakdown.offset)
            )
        }
    }
}

@Composable
private fun CalculationRow(title: String, detail: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp).semantics(mergeDescendants = true) {}
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun signedKcal(offset: Int): String = when {
    offset > 0 -> "+" + kcalText(offset.toDouble())
    offset < 0 -> "−" + kcalText(-offset.toDouble())
    else -> kcalText(0.0)
}

// Calories saisies à la main, à la place du calcul (ou faute de profil complet).
@Composable
private fun ManualKcalField(viewModel: NeedsViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val calculated = profile?.let { viewModel.breakdown(it) }?.goalKcal
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GoalLine(viewModel.target(profile ?: NeedsProfile())?.kcal ?: calculated)
        OutlinedTextField(
            value = viewModel.manualKcalText,
            onValueChange = viewModel::onManualKcalChange,
            label = { Text(stringResource(R.string.nutrition_manual_kcal)) },
            placeholder = calculated?.let { { Text(wholeNumber(it.toDouble())) } },
            isError = !viewModel.isManualKcalValid(),
            supportingText = {
                Text(
                    stringResource(
                        if (viewModel.isManualKcalValid()) {
                            R.string.nutrition_manual_kcal_hint
                        } else {
                            R.string.nutrition_manual_kcal_error
                        }
                    )
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (viewModel.manualKcalText.isNotEmpty() && calculated != null) {
            LiftTextButton(
                text = stringResource(R.string.nutrition_reset_calculation),
                onClick = viewModel::resetToCalculation
            )
        }
    }
}

// « TON OBJECTIF   3 030 kcal / jour », en gros.
@Composable
private fun GoalLine(kcal: Int?) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(R.string.nutrition_goal_label).uppercase(Locale.FRENCH),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).padding(bottom = 6.dp)
        )
        Text(
            kcal?.let { wholeNumber(it.toDouble()) } ?: stringResource(R.string.nutrition_needs_missing),
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            " " + stringResource(R.string.nutrition_kcal_per_day),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun MacroCard(
    target: NutritionTarget,
    proteinPerKg: Double,
    fatPerKg: Double,
    onStepProtein: (Int) -> Unit,
    onStepFat: (Int) -> Unit
) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(stringResource(R.string.nutrition_split))
            SplitBar(target)
            MacroRow(
                label = stringResource(R.string.nutrition_protein),
                color = proteinColor(),
                grams = target.proteinGrams,
                perKg = proteinPerKg,
                onStep = onStepProtein
            )
            HorizontalDivider()
            MacroRow(
                label = stringResource(R.string.nutrition_fat),
                color = fatColor(),
                grams = target.fatGrams,
                perKg = fatPerKg,
                onStep = onStepFat
            )
            HorizontalDivider()
            MacroRow(
                label = stringResource(R.string.nutrition_carbs),
                color = carbsColor(),
                grams = target.carbsGrams,
                perKg = null,
                onStep = null
            )
        }
    }
}

// Part de chaque macro dans les calories : protéines et glucides 4 kcal/g, lipides 9 kcal/g.
@Composable
private fun SplitBar(target: NutritionTarget) {
    val parts = listOf(
        target.proteinGrams * 4f to proteinColor(),
        target.fatGrams * 9f to fatColor(),
        target.carbsGrams * 4f to carbsColor()
    ).filter { it.first > 0f }
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth().height(10.dp)
    ) {
        parts.forEach { (kcal, color) ->
            Box(
                Modifier
                    .weight(kcal)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun MacroRow(label: String, color: Color, grams: Int, perKg: Double?, onStep: ((Int) -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.padding(end = 2.dp).clip(RoundedCornerShape(50)).background(color).padding(5.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (perKg != null && onStep != null) {
            IconButton(onClick = { onStep(-1) }) {
                Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.nutrition_decrease_per_kg, label))
            }
            Text(
                stringResource(R.string.nutrition_per_kg, perKgText(perKg)),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = { onStep(1) }) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.nutrition_increase_per_kg, label))
            }
        } else {
            Text(
                stringResource(R.string.nutrition_rest),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(R.string.nutrition_grams, wholeNumber(grams.toDouble())),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@StringRes
private fun ActivityLevel.labelRes(): Int = when (this) {
    ActivityLevel.SEDENTARY -> R.string.nutrition_activity_sedentary
    ActivityLevel.LIGHT -> R.string.nutrition_activity_light
    ActivityLevel.ACTIVE -> R.string.nutrition_activity_active
    ActivityLevel.VERY_ACTIVE -> R.string.nutrition_activity_very_active
    ActivityLevel.EXTREME -> R.string.nutrition_activity_extreme
}

@StringRes
private fun ActivityLevel.descriptionRes(): Int = when (this) {
    ActivityLevel.SEDENTARY -> R.string.nutrition_activity_sedentary_text
    ActivityLevel.LIGHT -> R.string.nutrition_activity_light_text
    ActivityLevel.ACTIVE -> R.string.nutrition_activity_active_text
    ActivityLevel.VERY_ACTIVE -> R.string.nutrition_activity_very_active_text
    ActivityLevel.EXTREME -> R.string.nutrition_activity_extreme_text
}

@StringRes
private fun NutritionObjective.labelRes(): Int = when (this) {
    NutritionObjective.CUT -> R.string.nutrition_objective_cut
    NutritionObjective.MAINTAIN -> R.string.nutrition_objective_maintain
    NutritionObjective.BULK -> R.string.nutrition_objective_bulk
}

@StringRes
private fun NutritionObjective.descriptionRes(): Int = when (this) {
    NutritionObjective.CUT -> R.string.nutrition_objective_cut_text
    NutritionObjective.MAINTAIN -> R.string.nutrition_objective_maintain_text
    NutritionObjective.BULK -> R.string.nutrition_objective_bulk_text
}

// « 2,0 » : toujours une décimale, les réglages vont de 0,1 en 0,1.
private fun perKgText(value: Double): String = String.format(Locale.FRENCH, "%.1f", value)
