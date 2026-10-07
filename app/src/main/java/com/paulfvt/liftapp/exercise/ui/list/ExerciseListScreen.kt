package com.paulfvt.liftapp.exercise.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.MuscleGroup
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.exercise.domain.MuscleFilter
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail

@Composable
fun ExerciseListScreen(
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: ExerciseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseListContent(
        uiState = uiState,
        searchText = viewModel.searchText,
        onSearchTextChange = viewModel::onSearchTextChange,
        onMuscleFilterChange = viewModel::onMuscleFilterChange,
        onEquipmentFilterChange = viewModel::onEquipmentFilterChange,
        onToggleShowHidden = viewModel::onToggleShowHidden,
        onBack = onBack,
        onExerciseClick = onExerciseClick,
        onCreateClick = onCreateClick
    )
}

// Version sans ViewModel, plus simple à prévisualiser et à tester.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseListContent(
    uiState: ExerciseListUiState,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onMuscleFilterChange: (MuscleFilter?) -> Unit,
    onEquipmentFilterChange: (Equipment?) -> Unit,
    onToggleShowHidden: () -> Unit,
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onCreateClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.exercises_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    MoreMenu(
                        showHidden = uiState.filters.showHidden,
                        onToggleShowHidden = onToggleShowHidden
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.exercises_create)) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(text = searchText, onTextChange = onSearchTextChange)
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MuscleFilterChip(selected = uiState.filters.muscle, onSelect = onMuscleFilterChange)
                EquipmentFilterChip(
                    selected = uiState.filters.equipment,
                    onSelect = onEquipmentFilterChange
                )
            }
            when {
                uiState.isLoading -> CenteredMessage(
                    stringResource(R.string.exercises_loading),
                    showProgress = true
                )
                uiState.exercises.isEmpty() -> CenteredMessage(
                    stringResource(R.string.exercises_empty)
                )
                else -> LazyColumn(
                    // Marge basse pour que le dernier exercice ne soit pas caché sous le bouton flottant.
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(uiState.exercises, key = { it.id }) { exercise ->
                        ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(text: String, onTextChange: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = { Text(stringResource(R.string.exercises_search)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = { onTextChange("") }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        stringResource(R.string.action_clear)
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun MoreMenu(showHidden: Boolean, onToggleShowHidden: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (showHidden) R.string.exercises_hide_hidden else R.string.exercises_show_hidden
                        )
                    )
                },
                onClick = {
                    expanded = false
                    onToggleShowHidden()
                }
            )
        }
    }
}

@Composable
private fun MuscleFilterChip(selected: MuscleFilter?, onSelect: (MuscleFilter?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        null -> stringResource(R.string.exercises_filter_muscle)
        is MuscleFilter.Group -> stringResource(selected.group.labelRes)
        is MuscleFilter.Single -> stringResource(selected.muscle.labelRes)
    }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null)
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fun select(filter: MuscleFilter?) {
                expanded = false
                onSelect(filter)
            }
            DropdownMenuItem(text = {
                Text(stringResource(R.string.exercises_filter_all))
            }, onClick = { select(null) })
            // Chaque groupe (en gras) est suivi de ses muscles, décalés vers la droite.
            MuscleGroup.entries.forEach { group ->
                DropdownMenuItem(
                    text = { Text(stringResource(group.labelRes), fontWeight = FontWeight.Bold) },
                    onClick = { select(MuscleFilter.Group(group)) }
                )
                Muscle.entries.filter { it.group == group }.forEach { muscle ->
                    // Un groupe d'un seul muscle portant le même nom (Pectoraux, Épaules…) n'est pas répété.
                    if (Muscle.entries.count { it.group == group } > 1) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(muscle.labelRes),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            },
                            onClick = { select(MuscleFilter.Single(muscle)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentFilterChip(selected: Equipment?, onSelect: (Equipment?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = {
                Text(stringResource(selected?.labelRes ?: R.string.exercises_filter_equipment))
            },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null)
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exercises_filter_all)) },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            Equipment.entries.forEach { equipment ->
                DropdownMenuItem(
                    text = { Text(stringResource(equipment.labelRes)) },
                    onClick = {
                        expanded = false
                        onSelect(equipment)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    val muscles = exercise.primaryMuscles.map { stringResource(it.labelRes) }
    val subtitle = (muscles + stringResource(exercise.equipment.labelRes)).joinToString(" · ")
    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = { Text(subtitle, maxLines = 1) },
        leadingContent = { ExerciseThumbnail(exercise) },
        trailingContent = {
            val tag = when {
                exercise.isHidden -> R.string.exercise_hidden_label
                exercise.isCustom -> R.string.exercise_custom_label
                else -> null
            }
            if (tag != null) {
                Text(
                    text = stringResource(tag),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun CenteredMessage(message: String, showProgress: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showProgress) CircularProgressIndicator()
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
