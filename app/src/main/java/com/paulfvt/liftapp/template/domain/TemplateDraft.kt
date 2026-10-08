package com.paulfvt.liftapp.template.domain

import com.paulfvt.liftapp.core.domain.moved
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType

const val DEFAULT_SET_COUNT = 3
const val MIN_SET_COUNT = 1
const val MAX_SET_COUNT = 20
const val DEFAULT_REST_SECONDS = 120
private const val MAX_TARGET_REPS_DIGITS = 3

// Choix proposés pour le temps de repos d'un exercice, en secondes.
val REST_CHOICES_SECONDS = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)

// Série prévue. Les reps cibles sont facultatives : null = case laissée vide.
data class TemplateSetDraft(val type: SetType = SetType.WORK, val targetReps: Int? = null)

// Un exercice ajouté commence avec 3 séries de travail sans cible de reps.
data class TemplateExerciseDraft(
    val exercise: Exercise,
    val sets: List<TemplateSetDraft> = List(DEFAULT_SET_COUNT) { TemplateSetDraft() },
    val restSeconds: Int = DEFAULT_REST_SECONDS
)

// Template en cours d'édition, gardé en mémoire jusqu'à l'enregistrement.
data class TemplateDraft(val name: String = "", val exercises: List<TemplateExerciseDraft> = emptyList())

fun TemplateDraft.nameError(): NameError? = validateName(name)

// Un exercice n'apparaît qu'une fois par template : ceux déjà présents sont ignorés.
fun TemplateDraft.addExercises(new: List<Exercise>): TemplateDraft {
    val present = exercises.map { it.exercise.id }.toSet()
    val added = new.distinctBy { it.id }.filter { it.id !in present }.map { TemplateExerciseDraft(it) }
    return copy(exercises = exercises + added)
}

fun TemplateDraft.removeExercise(exerciseId: Long): TemplateDraft =
    copy(exercises = exercises.filterNot { it.exercise.id == exerciseId })

// Place l'exercice fromId à la position actuelle de toId (glisser-déposer).
fun TemplateDraft.moveExercise(fromId: Long, toId: Long): TemplateDraft {
    val from = exercises.indexOfFirst { it.exercise.id == fromId }
    val to = exercises.indexOfFirst { it.exercise.id == toId }
    if (from == -1 || to == -1) return this
    return copy(exercises = exercises.moved(from, to))
}

fun TemplateDraft.updateExercise(
    exerciseId: Long,
    transform: (TemplateExerciseDraft) -> TemplateExerciseDraft
): TemplateDraft = copy(exercises = exercises.map { if (it.exercise.id == exerciseId) transform(it) else it })

// Une série ajoutée est toujours une série de travail sans cible, jusqu'à MAX_SET_COUNT séries.
fun TemplateExerciseDraft.addSet(): TemplateExerciseDraft =
    if (sets.size >= MAX_SET_COUNT) this else copy(sets = sets + TemplateSetDraft())

// Un exercice garde au moins une série.
fun TemplateExerciseDraft.removeSet(index: Int): TemplateExerciseDraft {
    if (sets.size <= MIN_SET_COUNT || index !in sets.indices) return this
    return copy(sets = sets.filterIndexed { i, _ -> i != index })
}

fun TemplateExerciseDraft.updateSet(
    index: Int,
    transform: (TemplateSetDraft) -> TemplateSetDraft
): TemplateExerciseDraft = copy(sets = sets.mapIndexed { i, set -> if (i == index) transform(set) else set })

// Saisie des reps cibles : chiffres seulement, 3 au maximum. Vide ou 0 = pas de cible.
fun parseTargetReps(input: String): Int? =
    input.filter(Char::isDigit).take(MAX_TARGET_REPS_DIGITS).toIntOrNull()?.takeIf { it > 0 }
