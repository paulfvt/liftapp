package com.paulfvt.liftapp.exercise.domain

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.MuscleGroup
import java.text.Normalizer
import java.util.Locale

private val DIACRITICS = Regex("\\p{M}+")
private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")

// Simplifie un texte pour la recherche : « Développé couché » devient « developpe couche ».
// NFD sépare chaque lettre de son accent, puis on supprime les accents (\p{M}).
fun normalizeForSearch(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(DIACRITICS, "")
    .lowercase(Locale.ROOT)
    .replace("œ", "oe")
    .replace("æ", "ae")
    .replace(NON_ALPHANUMERIC, " ")
    .trim()

sealed interface MuscleFilter {
    data class Group(val group: MuscleGroup) : MuscleFilter

    data class Single(val muscle: Muscle) : MuscleFilter
}

data class ExerciseQuery(
    val text: String = "",
    val muscle: MuscleFilter? = null,
    val equipment: Equipment? = null,
    val showHidden: Boolean = false
)

// Chaque mot tapé doit apparaître dans le nom, dans n'importe quel ordre : « couche dev » trouve
// « Développé couché ». Le filtre muscle ne regarde que les muscles principaux.
fun Exercise.matches(query: ExerciseQuery, normalizedName: String = normalizeForSearch(name)): Boolean {
    if (isHidden && !query.showHidden) return false
    if (query.equipment != null && equipment != query.equipment) return false
    val muscleMatches = when (val filter = query.muscle) {
        null -> true
        is MuscleFilter.Group -> primaryMuscles.any { it.group == filter.group }
        is MuscleFilter.Single -> filter.muscle in primaryMuscles
    }
    if (!muscleMatches) return false
    val words = normalizeForSearch(query.text).split(' ').filter { it.isNotEmpty() }
    return words.all { it in normalizedName }
}
