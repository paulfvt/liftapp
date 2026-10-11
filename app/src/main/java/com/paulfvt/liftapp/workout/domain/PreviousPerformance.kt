package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.template.domain.DEFAULT_SET_COUNT

// Associe à chaque série prévue une série de la dernière fois, par type : la n-ième série d'un type reprend
// la n-ième série de ce type, sinon la dernière série de ce type. Null si ce type n'a pas été fait la dernière fois.
fun matchPrevious(types: List<SetType>, previous: List<PreviousSet>): List<PreviousSet?> {
    val previousByType = previous.groupBy { it.type }
    val countByType = mutableMapOf<SetType, Int>()
    return types.map { type ->
        val index = countByType.getOrDefault(type, 0)
        countByType[type] = index + 1
        previousByType[type]?.let { sameType -> sameType.getOrNull(index) ?: sameType.last() }
    }
}

// Séries prévues (type et cible du template) pré-remplies avec les valeurs de la dernière fois.
fun prefill(planned: List<PlannedSet>, previous: List<PreviousSet>): List<PlannedSet> =
    planned.zip(matchPrevious(planned.map { it.type }, previous)) { set, match ->
        set.copy(values = match?.values ?: SetValues())
    }

// Exercice ajouté pendant la séance : mêmes séries que la dernière fois, sinon 3 séries de travail vides.
fun setsForAddedExercise(previous: List<PreviousSet>): List<PlannedSet> = if (previous.isEmpty()) {
    List(DEFAULT_SET_COUNT) { PlannedSet(SetType.WORK) }
} else {
    previous.map { PlannedSet(it.type, values = it.values) }
}

// Série ajoutée avec « + Ajouter une série » : une série de travail sans cible, pré-remplie comme les autres.
fun addedSet(currentTypes: List<SetType>, previous: List<PreviousSet>): PlannedSet {
    val types = currentTypes + SetType.WORK
    return PlannedSet(SetType.WORK, values = matchPrevious(types, previous).last()?.values ?: SetValues())
}
