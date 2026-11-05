package com.paulfvt.liftapp.nutrition.ui

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.mlkit.common.MlKitException
import com.paulfvt.liftapp.nutrition.data.LabelScanner
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.FOOD_NAME_MAX_LENGTH
import com.paulfvt.liftapp.nutrition.domain.FoodDraft
import com.paulfvt.liftapp.nutrition.domain.FoodDraftError
import com.paulfvt.liftapp.nutrition.domain.errors
import com.paulfvt.liftapp.nutrition.domain.toDraft
import com.paulfvt.liftapp.nutrition.navigation.FoodEditRoute
import com.paulfvt.liftapp.workout.domain.filterDecimalInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
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
    savedStateHandle: SavedStateHandle,
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

    private var hasTriedToSave = false
    private var isSaving = false

    init {
        if (foodId != null) {
            viewModelScope.launch { repository.observeFood(foodId).first()?.let { draft = it.toDraft() } }
        }
    }

    // Fichier que l'appareil photo d'Android remplit.
    fun labelPhotoUri(): Uri = scanner.photoUri()

    // Photo prise (ou annulée) : lue puis effacée, elle n'est jamais gardée.
    fun onLabelPhotoTaken(isTaken: Boolean) {
        if (isTaken) readLabel(scanner.photoUri(), isTemporary = true) else scanner.deletePhoto()
    }

    // Image choisie dans la galerie : seulement lue.
    fun onLabelImagePicked(uri: Uri) = readLabel(uri, isTemporary = false)

    private fun readLabel(uri: Uri, isTemporary: Boolean) {
        scanState = LabelScanState.Reading
        viewModelScope.launch {
            scanState = try {
                val reading = scanner.read(uri)
                update { reading.applyTo(this) }
                when {
                    reading.perPortionOnly -> LabelScanState.PerPortionOnly
                    reading.foundCount == 0 -> LabelScanState.NothingFound
                    else -> LabelScanState.Found(reading.foundCount)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Étiquette illisible", e)
                LabelScanState.Failed
            } catch (e: MlKitException) {
                Log.w(TAG, "Étiquette illisible", e)
                LabelScanState.Failed
            } finally {
                if (isTemporary) scanner.deletePhoto()
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
