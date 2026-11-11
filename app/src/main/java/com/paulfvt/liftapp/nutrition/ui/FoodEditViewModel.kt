package com.paulfvt.liftapp.nutrition.ui

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.mlkit.common.MlKitException
import com.paulfvt.liftapp.nutrition.data.LabelScanner
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.EnergyMismatch
import com.paulfvt.liftapp.nutrition.domain.FOOD_NAME_MAX_LENGTH
import com.paulfvt.liftapp.nutrition.domain.FoodDraft
import com.paulfvt.liftapp.nutrition.domain.FoodDraftError
import com.paulfvt.liftapp.nutrition.domain.LabelField
import com.paulfvt.liftapp.nutrition.domain.LabelReading
import com.paulfvt.liftapp.nutrition.domain.energyMismatch
import com.paulfvt.liftapp.nutrition.domain.errors
import com.paulfvt.liftapp.nutrition.domain.toDraft
import com.paulfvt.liftapp.nutrition.navigation.FoodEditRoute
import com.paulfvt.liftapp.nutrition.navigation.LABEL_IMAGE_KEY
import com.paulfvt.liftapp.nutrition.navigation.LABEL_VALUES_KEY
import com.paulfvt.liftapp.nutrition.navigation.labelReadingOf
import com.paulfvt.liftapp.workout.domain.filterDecimalInput
import com.paulfvt.liftapp.workout.domain.formatDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Ce qui s'est passé, pour que l'écran sache où aller ensuite.
sealed interface FoodEditResult {
    data class Created(val foodId: Long) : FoodEditResult

    data object Saved : FoodEditResult

    data object Deleted : FoodEditResult
}

// Lecture d'une étiquette en cours ou terminée, pour le message affiché sous les boutons.
sealed interface LabelScanState {
    data object Reading : LabelScanState

    data class Found(val count: Int) : LabelScanState

    data object PerPortionOnly : LabelScanState

    data object NothingFound : LabelScanState

    data object Failed : LabelScanState
}

// Création ou modification d'un aliment personnel (valeurs pour 100 g), avec lecture possible de l'étiquette.
@HiltViewModel
class FoodEditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository,
    private val scanner: LabelScanner
) : ViewModel() {
    private val foodId = savedStateHandle.toRoute<FoodEditRoute>().foodId
    val isEditing: Boolean = foodId != null

    var draft by mutableStateOf(FoodDraft())
        private set

    // Les erreurs ne s'affichent qu'après un premier essai d'enregistrement.
    var errors by mutableStateOf(emptySet<FoodDraftError>())
        private set

    var result by mutableStateOf<FoodEditResult?>(null)
        private set

    var isDeleteAsked by mutableStateOf(false)
        private set

    var scanState by mutableStateOf<LabelScanState?>(null)
        private set

    // Fibres de la dernière étiquette lue, pour le contrôle des calories (2 kcal par gramme).
    private var labelFiber = 0.0

    // Après une lecture d'étiquette : calories qui ne collent pas avec les macros. Recalculé à chaque correction, la
    // carte disparaît dès que les valeurs collent.
    val energyMismatch: EnergyMismatch?
        get() = if (scanState is LabelScanState.Found) draft.energyMismatch(labelFiber) else null

    private var hasTriedToSave = false
    private var isSaving = false

    init {
        if (foodId != null) {
            viewModelScope.launch { repository.observeFood(foodId).first()?.let { draft = it.toDraft() } }
        }
        // Ce que le scan en direct rend en se fermant (voir LabelScanRoute) : les valeurs retenues, ou une image
        // choisie à la place. Effacé aussitôt lu, pour ne pas resservir après une rotation.
        viewModelScope.launch {
            savedStateHandle.getStateFlow<DoubleArray?>(LABEL_VALUES_KEY, null).filterNotNull().collect { values ->
                savedStateHandle[LABEL_VALUES_KEY] = null
                onLabelRead(labelReadingOf(values))
            }
        }
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>(LABEL_IMAGE_KEY, null).filterNotNull().collect { uri ->
                savedStateHandle[LABEL_IMAGE_KEY] = null
                onLabelImagePicked(uri.toUri())
            }
        }
    }

    // Image choisie dans la galerie : seulement lue.
    fun onLabelImagePicked(uri: Uri) {
        scanState = LabelScanState.Reading
        viewModelScope.launch {
            try {
                onLabelRead(scanner.read(uri))
            } catch (e: IOException) {
                Log.w(TAG, "Étiquette illisible", e)
                scanState = LabelScanState.Failed
            } catch (e: MlKitException) {
                Log.w(TAG, "Étiquette illisible", e)
                scanState = LabelScanState.Failed
            }
        }
    }

    private fun onLabelRead(reading: LabelReading) {
        labelFiber = reading.fiber ?: 0.0
        update { reading.applyTo(this) }
        scanState = when {
            reading.perPortionOnly -> LabelScanState.PerPortionOnly
            reading.foundCount == 0 -> LabelScanState.NothingFound
            else -> LabelScanState.Found(reading.foundCount)
        }
    }

    // Met la valeur qui explique l'écart de calories à la place du chiffre mal lu.
    fun applyEnergySuggestion() {
        val mismatch = energyMismatch ?: return
        val value = formatDecimal(mismatch.suggestion ?: return)
        update {
            when (mismatch.suspect) {
                LabelField.ENERGY -> copy(kcal = value)
                LabelField.PROTEIN -> copy(protein = value)
                LabelField.CARBS -> copy(carbs = value)
                LabelField.FAT -> copy(fat = value)
                null -> this
            }
        }
    }

    fun onNameChange(value: String) = update { copy(name = value.take(FOOD_NAME_MAX_LENGTH)) }

    fun onKcalChange(value: String) = update { copy(kcal = decimal(value)) }

    fun onProteinChange(value: String) = update { copy(protein = decimal(value)) }

    fun onCarbsChange(value: String) = update { copy(carbs = decimal(value)) }

    fun onFatChange(value: String) = update { copy(fat = decimal(value)) }

    fun save() {
        hasTriedToSave = true
        errors = draft.errors()
        if (errors.isNotEmpty() || isSaving) return
        isSaving = true
        viewModelScope.launch {
            result = if (foodId != null) {
                repository.updateFood(foodId, draft)
                FoodEditResult.Saved
            } else {
                FoodEditResult.Created(repository.createFood(draft))
            }
        }
    }

    fun askDelete() {
        isDeleteAsked = true
    }

    fun dismissDelete() {
        isDeleteAsked = false
    }

    fun delete() {
        val id = foodId ?: return
        isDeleteAsked = false
        viewModelScope.launch {
            repository.removeFood(id)
            result = FoodEditResult.Deleted
        }
    }

    private fun update(change: FoodDraft.() -> FoodDraft) {
        draft = draft.change()
        if (hasTriedToSave) errors = draft.errors()
    }

    private fun decimal(value: String) = filterDecimalInput(value, allowNegative = false)

    private companion object {
        const val TAG = "FoodEditViewModel"
    }
}
