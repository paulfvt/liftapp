package com.paulfvt.liftapp.nutrition.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.nutrition.domain.FoodDraftError
import com.paulfvt.liftapp.template.ui.ConfirmDeleteDialog

// Aliment personnel : nom et valeurs pour 100 g, comme sur l'étiquette d'un produit.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditScreen(
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: FoodEditViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.result) {
        when (val result = viewModel.result) {
            null -> Unit
            is FoodEditResult.Created -> onCreated(result.foodId)
            FoodEditResult.Saved -> onSaved()
            FoodEditResult.Deleted -> onDeleted()
        }
    }
    val draft = viewModel.draft
    val errors = viewModel.errors
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isEditing) R.string.nutrition_edit_food else R.string.nutrition_new_food
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.nutrition_food_name)) },
                isError = FoodDraftError.NAME_MISSING in errors,
                supportingText = errorText(FoodDraftError.NAME_MISSING in errors, R.string.nutrition_error_name),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.nutrition_food_values_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NumberField(
                draft.kcal,
                viewModel::onKcalChange,
                R.string.nutrition_food_kcal,
                FoodDraftError.KCAL_INVALID in errors,
                R.string.nutrition_error_kcal
            )
            NumberField(
                draft.protein,
                viewModel::onProteinChange,
                R.string.nutrition_food_protein,
                FoodDraftError.PROTEIN_INVALID in errors,
                R.string.nutrition_error_grams_per_100g
            )
            NumberField(
                draft.carbs,
                viewModel::onCarbsChange,
                R.string.nutrition_food_carbs,
                FoodDraftError.CARBS_INVALID in errors,
                R.string.nutrition_error_grams_per_100g
            )
            NumberField(
                draft.fat,
                viewModel::onFatChange,
                R.string.nutrition_food_fat,
                FoodDraftError.FAT_INVALID in errors,
                R.string.nutrition_error_grams_per_100g,
                imeAction = ImeAction.Done
            )
            if (FoodDraftError.MACROS_OVER_100G in errors) {
                Text(
                    stringResource(R.string.nutrition_error_macros_sum),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (viewModel.isEditing) {
                Text(
                    stringResource(R.string.nutrition_food_edit_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SignalButton(
                text = stringResource(if (viewModel.isEditing) R.string.action_save else R.string.action_create),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            )
            if (viewModel.isEditing) {
                LiftTextButton(
                    text = stringResource(R.string.nutrition_delete_food),
                    onClick = viewModel::askDelete,
                    icon = R.drawable.ic_delete,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    if (viewModel.isDeleteAsked) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.nutrition_delete_food_title, draft.name),
            text = stringResource(R.string.nutrition_delete_food_text),
            onConfirm = viewModel::delete,
            onDismiss = viewModel::dismissDelete
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    isError: Boolean,
    @StringRes error: Int,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        isError = isError,
        supportingText = errorText(isError, error),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun errorText(show: Boolean, @StringRes text: Int): (@Composable () -> Unit)? = if (show) {
    { Text(stringResource(text)) }
} else {
    null
}
