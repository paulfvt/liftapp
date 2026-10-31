package com.paulfvt.liftapp.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionBlock
import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.ui.durationLabel
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.progress.ui.WeeklyGoalStepper
import com.paulfvt.liftapp.settings.data.ThemeMode
import com.paulfvt.liftapp.template.domain.REST_CHOICES_SECONDS
import com.paulfvt.liftapp.template.ui.ChoiceDialog

// Réglages (cahier § 4.11), ouverts depuis le Profil : affichage, entraînement, confidentialité et données.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRestDialogOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.settings_title)) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionBlock(stringResource(R.string.settings_display)) {
                SettingsCard {
                    ChoiceRow(
                        label = stringResource(R.string.settings_theme),
                        options = ThemeMode.entries,
                        selected = uiState.app.themeMode,
                        optionLabel = { stringResource(it.labelRes()) },
                        onSelect = viewModel::onThemeModeChange
                    )
                    UnitChoices(
                        units = uiState.app.units,
                        onWeightUnitChange = viewModel::onWeightUnitChange,
                        onLengthUnitChange = viewModel::onLengthUnitChange
                    )
                }
            }
            SectionBlock(stringResource(R.string.settings_training)) {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.weekly_goal_dialog_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        WeeklyGoalStepper(uiState.weeklyGoal, onGoalChange = viewModel::onWeeklyGoalChange)
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_default_rest)) },
                        supportingContent = { Text(durationLabel(uiState.app.defaultRestSeconds)) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.clickable(role = Role.Button) { isRestDialogOpen = true }
                    )
                    SwitchRow(
                        label = stringResource(R.string.workout_rest_sound),
                        checked = uiState.workout.restSound,
                        onCheckedChange = viewModel::onRestSoundChange
                    )
                    SwitchRow(
                        label = stringResource(R.string.workout_rest_vibration),
                        checked = uiState.workout.restVibration,
                        onCheckedChange = viewModel::onRestVibrationChange
                    )
                }
            }
        }
    }
    if (isRestDialogOpen) {
        ChoiceDialog(
            title = stringResource(R.string.settings_default_rest),
            options = REST_CHOICES_SECONDS,
            selected = uiState.app.defaultRestSeconds,
            label = { durationLabel(it) },
            onSelect = { seconds ->
                viewModel.onDefaultRestChange(seconds)
                isRestDialogOpen = false
            },
            onDismiss = { isRestDialogOpen = false }
        )
    }
}

// Poids en kilos ou en livres, longueurs en centimètres ou en pouces : aussi sur l'écran de bienvenue.
@Composable
fun UnitChoices(units: Units, onWeightUnitChange: (WeightUnit) -> Unit, onLengthUnitChange: (LengthUnit) -> Unit) {
    ChoiceRow(
        label = stringResource(R.string.settings_weight_unit),
        options = WeightUnit.entries,
        selected = units.weight,
        optionLabel = { stringResource(it.labelRes) },
        onSelect = onWeightUnitChange
    )
    ChoiceRow(
        label = stringResource(R.string.settings_length_unit),
        options = LengthUnit.entries,
        selected = units.length,
        optionLabel = { stringResource(it.labelRes) },
        onSelect = onLengthUnitChange
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) { Column(content = content) }
}

// Ligne à interrupteur : toute la ligne est cliquable (48 dp au moins) et lue comme un seul interrupteur.
@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, supporting: String? = null) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    )
}

// Choix exclusif en boutons segmentés (thème, unités).
@Composable
fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) { Text(optionLabel(option)) }
            }
        }
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
