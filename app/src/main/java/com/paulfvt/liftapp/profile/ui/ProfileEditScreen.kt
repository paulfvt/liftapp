package com.paulfvt.liftapp.profile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.PastDateField
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.profile.domain.FIRST_NAME_MAX_LENGTH
import com.paulfvt.liftapp.profile.domain.ProfileError
import com.paulfvt.liftapp.profile.domain.Sex

private const val DEFAULT_AGE_SHOWN = 25L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(onClose: () -> Unit, viewModel: ProfileEditViewModel = hiltViewModel()) {
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
                title = { ScreenTitle(stringResource(R.string.profile_edit_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.profile_edit_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val firstNameError = ProfileError.FIRST_NAME_TOO_LONG in errors
            OutlinedTextField(
                value = draft.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text(stringResource(R.string.profile_first_name)) },
                isError = firstNameError,
                supportingText = if (firstNameError) {
                    {
                        Text(
                            pluralStringResource(
                                R.plurals.profile_error_first_name,
                                FIRST_NAME_MAX_LENGTH,
                                FIRST_NAME_MAX_LENGTH
                            )
                        )
                    }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            val heightError = ProfileError.HEIGHT_INVALID in errors
            OutlinedTextField(
                value = draft.height,
                onValueChange = viewModel::onHeightChange,
                label = { Text(stringResource(R.string.profile_height)) },
                suffix = { Text(stringResource(R.string.unit_cm)) },
                isError = heightError,
                supportingText = if (heightError) {
                    { Text(stringResource(R.string.profile_error_height)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            PastDateField(
                label = stringResource(R.string.profile_birth_date),
                date = draft.birthDate,
                today = viewModel.today,
                onChange = viewModel::onBirthDateChange,
                emptyText = stringResource(R.string.profile_not_set),
                clearDescription = stringResource(R.string.profile_clear_birth_date),
                errorText = if (ProfileError.BIRTH_DATE_IN_FUTURE in errors) {
                    stringResource(R.string.error_date_in_future)
                } else {
                    null
                },
                firstShown = viewModel.today.minusYears(DEFAULT_AGE_SHOWN)
            )
            SexField(selected = draft.sex, onSelect = viewModel::onSexChange)
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.template_discard_title)) },
            text = { Text(stringResource(R.string.profile_discard_text)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexField(selected: Sex?, onSelect: (Sex?) -> Unit) {
    val options = listOf(Sex.FEMALE, Sex.MALE, null)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.profile_sex),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(stringResource(sexLabel(option))) }
                )
            }
        }
    }
}

fun sexLabel(sex: Sex?): Int = when (sex) {
    Sex.FEMALE -> R.string.profile_sex_female
    Sex.MALE -> R.string.profile_sex_male
    null -> R.string.profile_sex_unspecified
}
