package com.paulfvt.liftapp.profile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.PastDateField
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.profile.domain.MeasurementError
import com.paulfvt.liftapp.profile.domain.validRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MeasurementEntryScreen(onClose: () -> Unit, viewModel: MeasurementEntryViewModel = hiltViewModel()) {
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onClose()
    }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val close = { if (viewModel.hasChanges) showDiscardDialog = true else onClose() }
    BackHandler(enabled = viewModel.hasChanges) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = close) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_cancel))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.measurement_new_title)) },
                actions = {
                    TextButton(onClick = viewModel::save) { Text(stringResource(R.string.action_save)) }
                }
            )
        }
    ) { padding ->
        val draft = viewModel.draft
        val errors = viewModel.errors
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            FieldLabel(stringResource(R.string.measurement_type))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurementType.entries.forEach { type ->
                    FilterChip(
                        selected = type == draft.type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(stringResource(type.labelRes)) }
                    )
                }
            }
            val range = draft.type.validRange()
            val valueError = when {
                MeasurementError.VALUE_MISSING in errors -> stringResource(R.string.measurement_error_missing)
                MeasurementError.VALUE_OUT_OF_RANGE in errors -> stringResource(
                    R.string.measurement_error_range,
                    measureText(draft.type, range.start),
                    measureText(draft.type, range.endInclusive)
                )
                else -> null
            }
            // Rappel de la dernière valeur : utile pour repérer une faute de frappe avant d'enregistrer.
            val previous = viewModel.latest[draft.type]
            val hint = previous?.let {
                stringResource(R.string.measurement_last, measureText(it.type, it.value), shortDay(it.measuredAt))
            }
            OutlinedTextField(
                value = draft.value,
                onValueChange = viewModel::onValueChange,
                label = { Text(stringResource(draft.type.labelRes)) },
                suffix = { Text(stringResource(draft.type.unitRes)) },
                isError = valueError != null,
                supportingText = (valueError ?: hint)?.let { text -> { Text(text) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            if (draft.type == MeasurementType.BODY_FAT) {
                MethodField(
                    selected = draft.method,
                    isError = MeasurementError.METHOD_MISSING in errors,
                    onSelect = viewModel::onMethodChange
                )
            }
            PastDateField(
                label = stringResource(R.string.measurement_date),
                date = draft.date,
                today = viewModel.today,
                onChange = viewModel::onDateChange,
                errorText = if (MeasurementError.DATE_IN_FUTURE in errors) {
                    stringResource(R.string.error_date_in_future)
                } else {
                    null
                }
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.template_discard_title)) },
            text = { Text(stringResource(R.string.measurement_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onClose()
                }) { Text(stringResource(R.string.template_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.template_discard_keep))
                }
            }
        )
    }
}

@Composable
private fun FieldLabel(text: String, isError: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Méthode de mesure de la masse grasse : balance, pince ou estimation.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodField(selected: BodyFatMethod?, isError: Boolean, onSelect: (BodyFatMethod) -> Unit) {
    val options = BodyFatMethod.entries
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(stringResource(R.string.measurement_method), isError)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(stringResource(option.labelRes)) }
                )
            }
        }
        if (isError) {
            Text(
                stringResource(R.string.measurement_error_method),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
