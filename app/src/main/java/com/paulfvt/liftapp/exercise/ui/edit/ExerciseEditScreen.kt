package com.paulfvt.liftapp.exercise.ui.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.exercise.domain.DraftError
import com.paulfvt.liftapp.exercise.domain.EXERCISE_NAME_MAX_LENGTH

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: ExerciseEditViewModel = hiltViewModel()) {
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onSaved()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScreenTitle(
                        stringResource(
                            if (viewModel.isNew) R.string.exercise_create_title else R.string.exercise_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            stringResource(R.string.action_cancel)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = viewModel.isLoaded) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { padding ->
        if (!viewModel.isLoaded) return@Scaffold
        val draft = viewModel.draft
        val errors = viewModel.errors
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val nameError = when {
                DraftError.NAME_EMPTY in errors -> R.string.exercise_error_name_empty
                DraftError.NAME_TOO_LONG in errors -> R.string.exercise_error_name_too_long
                else -> null
            }
            OutlinedTextField(
                value = draft.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.exercise_name)) },
                isError = nameError != null,
                supportingText = {
                    Text(
                        nameError?.let { stringResource(it) }
                            ?: "${draft.name.trim().length} / $EXERCISE_NAME_MAX_LENGTH"
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier.fillMaxWidth()
            )
            SelectField(
                labelRes = R.string.exercise_category,
                selected = draft.category,
                options = ExerciseCategory.entries,
                optionLabel = { it.labelRes },
                onSelect = viewModel::onCategoryChange
            )
            SelectField(
                labelRes = R.string.exercise_equipment,
                selected = draft.equipment,
                options = Equipment.entries,
                optionLabel = { it.labelRes },
                onSelect = viewModel::onEquipmentChange
            )
            SelectField(
                labelRes = R.string.exercise_tracking,
                selected = draft.trackingType,
                options = TrackingType.entries,
                optionLabel = { it.labelRes },
                onSelect = viewModel::onTrackingTypeChange
            )
            MusclePicker(
                titleRes = R.string.exercise_primary_muscles,
                selected = draft.primaryMuscles,
                onToggle = viewModel::onTogglePrimary,
                errorRes = R.string.exercise_error_no_primary.takeIf {
                    DraftError.NO_PRIMARY_MUSCLE in
                        errors
                }
            )
            MusclePicker(
                titleRes = R.string.exercise_secondary_muscles,
                selected = draft.secondaryMuscles,
                onToggle = viewModel::onToggleSecondary
            )
        }
    }
}

// Champ de sélection : une carte cliquable qui ouvre un menu déroulant.
@Composable
private fun <T> SelectField(
    @StringRes labelRes: Int,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> Int,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.DropdownList) { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(optionLabel(selected)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(optionLabel(option))) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusclePicker(
    @StringRes titleRes: Int,
    selected: Set<Muscle>,
    onToggle: (Muscle) -> Unit,
    @StringRes errorRes: Int? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
        if (errorRes != null) {
            Text(
                stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Muscle.entries.forEach { muscle ->
                FilterChip(
                    selected = muscle in selected,
                    onClick = { onToggle(muscle) },
                    label = { Text(stringResource(muscle.labelRes)) }
                )
            }
        }
    }
}
