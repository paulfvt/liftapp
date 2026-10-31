package com.paulfvt.liftapp.exercise.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ButtonLabel
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle

// Catalogue en mode sélection, ouvert depuis l'éditeur de template ou la séance en cours.
// Les exercices déjà présents sont grisés : un exercice n'y figure qu'une fois.
// En sélection simple (remplacer un exercice), un appui sur une ligne choisit l'exercice et ferme l'écran.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    excludedIds: Set<Long>,
    singleSelection: Boolean,
    onCancel: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    viewModel: ExerciseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Liste plutôt qu'ensemble : les exercices sont ajoutés dans l'ordre où ils ont été cochés.
    // rememberSaveable garde la sélection après une rotation de l'écran.
    var selectedIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScreenTitle(
                        stringResource(if (singleSelection) R.string.picker_replace_title else R.string.picker_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_cancel))
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onConfirm(selectedIds) },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = {
                        ButtonLabel(pluralStringResource(R.plurals.picker_confirm, selectedIds.size, selectedIds.size))
                    }
                )
            }
        }
    ) { padding ->
        ExerciseCatalog(
            uiState = uiState,
            searchText = viewModel.searchText,
            onSearchTextChange = viewModel::onSearchTextChange,
            onMuscleFilterChange = viewModel::onMuscleFilterChange,
            onEquipmentFilterChange = viewModel::onEquipmentFilterChange,
            modifier = Modifier.padding(padding)
        ) { exercise ->
            val alreadyAdded = exercise.id in excludedIds
            val selected = exercise.id in selectedIds
            ExerciseRow(
                exercise = exercise,
                trailing = {
                    if (alreadyAdded) {
                        Text(
                            stringResource(R.string.picker_already_added),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!singleSelection) {
                        // onCheckedChange null : c'est toute la ligne qui coche, cible tactile plus grande.
                        Checkbox(checked = selected, onCheckedChange = null)
                    }
                },
                modifier = if (singleSelection) {
                    Modifier.clickable(enabled = !alreadyAdded) { onConfirm(listOf(exercise.id)) }
                } else {
                    Modifier.toggleable(
                        value = selected,
                        enabled = !alreadyAdded,
                        role = Role.Checkbox,
                        onValueChange = { checked ->
                            selectedIds = if (checked) selectedIds + exercise.id else selectedIds - exercise.id
                        }
                    )
                }
            )
        }
    }
}
