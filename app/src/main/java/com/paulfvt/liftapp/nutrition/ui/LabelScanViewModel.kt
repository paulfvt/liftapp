package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.paulfvt.liftapp.nutrition.data.LabelScanner
import com.paulfvt.liftapp.nutrition.domain.LabelReading
import com.paulfvt.liftapp.nutrition.domain.LabelVotes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Scan en direct d'une étiquette : les lectures des images de la caméra votent, l'écran montre les valeurs retenues.
@HiltViewModel
class LabelScanViewModel @Inject constructor(scanner: LabelScanner) : ViewModel() {
    var votes by mutableStateOf(LabelVotes())
        private set

    // La dernière image lue ne donne que des valeurs par portion : l'écran l'explique.
    var isPerPortionOnly by mutableStateOf(false)
        private set

    val analyzer = scanner.analyzer(::onReading)

    private fun onReading(reading: LabelReading) {
        isPerPortionOnly = reading.perPortionOnly
        votes = votes.add(reading)
    }
}
