package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.TrackingType

// Pavé numérique de saisie des séries, façon Strong (décision du 2 novembre 2026).

// Une case d'une ligne de série : la première (poids, lest, distance) ou la seconde (reps, durée).
enum class FieldSlot {
    FIRST,
    SECOND
}

data class SetField(val setId: Long, val slot: FieldSlot)

// Cases d'une série selon le suivi : une durée seule n'a qu'une case, la seconde colonne.
fun slotsFor(trackingType: TrackingType): List<FieldSlot> = when (trackingType) {
    TrackingType.DURATION -> listOf(FieldSlot.SECOND)
    TrackingType.WEIGHT_REPS, TrackingType.REPS, TrackingType.DISTANCE -> listOf(FieldSlot.FIRST, FieldSlot.SECOND)
}

// Ordre de « Suivant » : exercice après exercice, série après série, de gauche à droite.
fun List<WorkoutExercise>.keypadFields(): List<SetField> = flatMap { item ->
    val slots = slotsFor(item.exercise.trackingType)
    item.sets.flatMap { set -> slots.map { SetField(set.id, it) } }
}

// Effet de « Suivant » sur `current` : la case suivante (null : plus rien, le pavé se ferme) et la série quittée,
// à valider, si `current` était sa dernière case.
data class KeypadMove(val next: SetField?, val finishedSetId: Long?)

fun List<SetField>.moveAfter(current: SetField): KeypadMove {
    val index = indexOf(current)
    // Case disparue entre-temps (série retirée) : le pavé se ferme.
    if (index == -1) return KeypadMove(next = null, finishedSetId = null)
    val next = getOrNull(index + 1)
    return KeypadMove(next, current.setId.takeIf { next?.setId != current.setId })
}

// Texte d'une case éditée au pavé. selected : valeur sélectionnée (ouverture de la case, − ou +), la prochaine
// touche la remplace au lieu de s'y ajouter.
data class KeypadText(val text: String, val selected: Boolean = text.isNotEmpty())

fun KeypadText.typed(key: Char, filter: (String) -> String): KeypadText =
    KeypadText(filter(if (selected) key.toString() else text + key), selected = false)

fun KeypadText.erased(): KeypadText = KeypadText(if (selected) "" else text.dropLast(1), selected = false)
