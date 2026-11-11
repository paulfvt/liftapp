package com.paulfvt.liftapp.nutrition.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTonalButton
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.nutrition.domain.EnergyMismatch
import com.paulfvt.liftapp.nutrition.domain.FoodDraft
import com.paulfvt.liftapp.nutrition.domain.FoodDraftError
import com.paulfvt.liftapp.nutrition.domain.LabelField
import com.paulfvt.liftapp.template.ui.ConfirmDeleteDialog
import com.paulfvt.liftapp.workout.domain.formatDecimal
import kotlin.math.roundToInt

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
    val context = LocalContext.current
    // Appareil photo d'Android : il écrit la photo dans le fichier temporaire de l'étiquette.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onLabelPhotoTaken)
    var isCameraDenied by rememberSaveable { mutableStateOf(false) }
    // L'app déclare la caméra (photos de progression) : Android exige alors la permission avant l'appareil photo.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) camera.launch(viewModel.labelPhotoUri()) else isCameraDenied = true
    }
    // Sélecteur de photos d'Android : aucune permission, l'app ne voit que l'image choisie.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onLabelImagePicked)
    }
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
            LabelScanSection(
                state = viewModel.scanState,
                isCameraDenied = isCameraDenied,
                onTakePhoto = {
                    isCameraDenied = false
                    if (context.hasCameraPermission()) {
                        camera.launch(viewModel.labelPhotoUri())
                    } else {
                        permission.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickImage = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
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
            viewModel.energyMismatch?.let { mismatch ->
                EnergyMismatchCard(mismatch, draft, onApply = viewModel::applyEnergySuggestion)
            }
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

// Photo de l'étiquette (décision du 5 novembre 2026) : boutons, puis où en est la lecture et ce qu'il faut vérifier.
@Composable
private fun LabelScanSection(
    state: LabelScanState?,
    isCameraDenied: Boolean,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit
) {
    val isReading = state == LabelScanState.Reading
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LiftOutlinedButton(
                text = stringResource(R.string.nutrition_scan_photo),
                onClick = onTakePhoto,
                enabled = !isReading,
                icon = R.drawable.ic_photo_camera
            )
            LiftTextButton(
                text = stringResource(R.string.nutrition_scan_gallery),
                onClick = onPickImage,
                enabled = !isReading,
                icon = R.drawable.ic_photo_library
            )
        }
        if (isReading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.nutrition_scan_reading), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val message = when {
                isCameraDenied -> stringResource(R.string.nutrition_scan_camera_denied)
                state is LabelScanState.Found && state.count == LABEL_VALUES ->
                    stringResource(R.string.nutrition_scan_all_found)
                state is LabelScanState.Found ->
                    pluralStringResource(R.plurals.nutrition_scan_partial, state.count, state.count)
                state == LabelScanState.PerPortionOnly -> stringResource(R.string.nutrition_scan_per_portion)
                state == LabelScanState.NothingFound -> stringResource(R.string.nutrition_scan_nothing)
                state == LabelScanState.Failed -> stringResource(R.string.nutrition_scan_failed)
                else -> stringResource(R.string.nutrition_scan_hint)
            }
            val isProblem = isCameraDenied || state != null && state !is LabelScanState.Found
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Calories qui ne collent pas avec les macros après une lecture d'étiquette : la valeur sans doute mal lue et, quand
// une seule correction explique l'écart, cette correction en un appui.
@Composable
private fun EnergyMismatchCard(mismatch: EnergyMismatch, draft: FoodDraft, onApply: () -> Unit) {
    val expected = formatDecimal(mismatch.expectedKcal.roundToInt().toDouble())
    val suggestion = mismatch.suggestion?.let(::formatDecimal)
    val field = mismatch.suspect
    val text = when {
        field == null || suggestion == null -> stringResource(R.string.nutrition_check_unknown, expected, draft.kcal)
        field == LabelField.ENERGY -> stringResource(R.string.nutrition_check_energy, expected, draft.kcal, suggestion)
        else -> stringResource(
            R.string.nutrition_check_macro,
            draft.textOf(field),
            stringResource(field.nameRes),
            expected,
            draft.kcal,
            suggestion
        )
    }
    LiftCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            Icon(
                painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.nutrition_check_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (field != null && suggestion != null) {
                    LiftTonalButton(
                        text = stringResource(
                            if (field == LabelField.ENERGY) {
                                R.string.nutrition_check_apply_kcal
                            } else {
                                R.string.nutrition_check_apply_grams
                            },
                            suggestion
                        ),
                        onClick = onApply,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun FoodDraft.textOf(field: LabelField): String = when (field) {
    LabelField.ENERGY -> kcal
    LabelField.PROTEIN -> protein
    LabelField.CARBS -> carbs
    LabelField.FAT -> fat
}

@get:StringRes
private val LabelField.nameRes: Int
    get() = when (this) {
        LabelField.ENERGY -> R.string.nutrition_food_kcal
        LabelField.PROTEIN -> R.string.nutrition_check_protein
        LabelField.CARBS -> R.string.nutrition_check_carbs
        LabelField.FAT -> R.string.nutrition_check_fat
    }

// Énergie, protéines, glucides et lipides.
private const val LABEL_VALUES = 4

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
