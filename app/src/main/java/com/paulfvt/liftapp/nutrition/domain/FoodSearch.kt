package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.exercise.domain.normalizeForSearch

// Recherche d'aliments en mémoire (environ 3 100), comme celle des exercices. Les noms Ciqual sont techniques
// (« Poulet, filet, sans peau, rôti/cuit au four ») : chaque mot tapé doit commencer un mot du nom, dans n'importe
// quel ordre, et le pluriel ne compte pas (« oeufs » trouve « Oeuf, cru »).
const val SEARCH_RESULT_LIMIT = 60

// Aliment avec ses mots déjà simplifiés, préparés une fois pour toute la liste.
class SearchableFood(val food: Food) {
    val words: List<String> = searchWords(food.name)
}

fun searchWords(text: String): List<String> =
    normalizeForSearch(text).split(' ').filter { it.isNotEmpty() }.map(::singular)

fun searchFoods(query: String, foods: List<SearchableFood>, limit: Int = SEARCH_RESULT_LIMIT): List<Food> {
    val tokens = searchWords(query)
    if (tokens.isEmpty()) return emptyList()
    return foods
        .filter { candidate -> tokens.all { token -> candidate.words.any { it.startsWith(token) } } }
        .sortedWith(
            // Favoris d'abord, puis les noms qui commencent par le premier mot tapé, puis les plus courts : « Riz »
            // passe avant « Salade de riz au thon ».
            compareByDescending<SearchableFood> { it.food.isFavorite }
                .thenByDescending { it.words.first().startsWith(tokens.first()) }
                .thenBy { it.words.size }
                .thenBy { it.food.name.length }
                .thenBy { it.food.name }
        )
        .take(limit)
        .map { it.food }
}

// Retire le s ou le x final des mots de plus de 3 lettres : « oeufs » → « oeuf », « noix » reste « noix ».
private fun singular(word: String): String {
    val isPlural = word.length > 3 && (word.endsWith('s') || word.endsWith('x')) && !word.endsWith("ix")
    return if (isPlural) word.dropLast(1) else word
}
