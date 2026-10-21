package com.paulfvt.liftapp.profile.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.profile.data.MeasurementRepository
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import com.paulfvt.liftapp.profile.domain.MeasurementDraft
import com.paulfvt.liftapp.profile.domain.MeasurementError
import com.paulfvt.liftapp.profile.domain.errors
import com.paulfvt.liftapp.profile.domain.toMeasurement
import com.paulfvt.liftapp.profile.navigation.MeasurementEntryRoute
import com.paulfvt.liftapp.workout.domain.filterDecimalInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Saisie d'une nouvelle mesure. Le type vient de l'écran d'origine (le poids par défaut).
@HiltViewModel
class MeasurementEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val measurements: MeasurementRepository,
    private val clock: Clock
) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    val today: LocalDate get() = clock.instant().atZone(zone).toLocalDate()

    private val initialType = savedStateHandle.toRoute<MeasurementEntryRoute>().type
        ?.let { name -> MeasurementType.entries.firstOrNull { it.name == name } }
        ?: MeasurementType.BODY_WEIGHT

    var draft by mutableStateOf(MeasurementDraft(type = initialType, date = today))
        private set

    val hasChanges: Boolean get() = draft.value.isNotBlank()

    // Dernière saisie de chaque type : rappel affiché sous le champ et méthode proposée pour la masse grasse.
    var latest by mutableStateOf(emptyMap<MeasurementType, BodyMeasurement>())
        private set

    // Les erreurs ne s'affichent qu'après un premier essai d'enregistrement.
    var errors by mutableStateOf(emptySet<MeasurementError>())
        private set

    var isSaved by mutableStateOf(false)
        private set

    private var hasTriedToSave = false
    private var isSaving = false

    init {
        viewModelScope.launch {
            latest = measurements.observeAll().first().groupBy { it.type }.mapValues { (_, list) -> list.first() }
            update { withDefaultMethod() }
        }
    }

    fun onTypeChange(type: MeasurementType) = update { copy(type = type).withDefaultMethod() }

    fun onValueChange(value: String) = update { copy(value = filterDecimalInput(value, allowNegative = false)) }

    fun onMethodChange(method: BodyFatMethod) = update { copy(method = method) }

    fun onDateChange(date: LocalDate?) = update { copy(date = date ?: today) }

    fun save() {
        hasTriedToSave = true
        errors = draft.errors(today)
        if (errors.isNotEmpty() || isSaving) return
        isSaving = true
        viewModelScope.launch {
            measurements.add(draft.toMeasurement(clock.instant(), zone))
            isSaved = true
        }
    }

    // Masse grasse : on reprend la méthode de la dernière fois, pour comparer des mesures faites de la même façon.
    private fun MeasurementDraft.withDefaultMethod(): MeasurementDraft =
        if (type == MeasurementType.BODY_FAT && method == null) {
            copy(method = latest[MeasurementType.BODY_FAT]?.method)
        } else {
            this
        }

    private fun update(transform: MeasurementDraft.() -> MeasurementDraft) {
        draft = draft.transform()
        if (hasTriedToSave) errors = draft.errors(today)
    }
}
