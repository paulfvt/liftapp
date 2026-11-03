package com.paulfvt.liftapp.nutrition.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodSearchTest {
    private var nextId = 1L

    private fun food(name: String, favorite: Boolean = false) =
        Food(nextId++, name, Nutrients(100.0, 10.0, 10.0, 1.0), isCustom = false, isFavorite = favorite)

    private val foods = listOf(
        food("Poulet, filet, sans peau, rôti/cuit au four"),
        food("Poulet, cuisse, viande et peau, rôtie/cuite au four"),
        food("Salade de riz au thon"),
        food("Riz basmati, cuit, non salé"),
        food("Riz"),
        food("Oeuf, cru"),
        food("Pâtes sèches, cuites, non salées"),
        food("Noix, séchée, cerneaux")
    ).map(::SearchableFood)

    private fun names(query: String, list: List<SearchableFood> = foods) = searchFoods(query, list).map { it.name }

    @Test
    fun `every typed word must start a word of the name, in any order`() {
        assertEquals(listOf("Poulet, filet, sans peau, rôti/cuit au four"), names("four filet poul"))
        assertTrue(names("poulet cuisine").isEmpty())
    }

    @Test
    fun `accents, case and plurals are ignored`() {
        assertEquals(listOf("Pâtes sèches, cuites, non salées"), names("PATE SECHE"))
        assertEquals(listOf("Oeuf, cru"), names("oeufs"))
        assertEquals(listOf("Oeuf, cru"), names("œuf"))
        assertEquals(listOf("Noix, séchée, cerneaux"), names("noix"))
    }

    @Test
    fun `names starting with the first word and shorter names come first`() {
        assertEquals(listOf("Riz", "Riz basmati, cuit, non salé", "Salade de riz au thon"), names("riz"))
    }

    @Test
    fun `favorites come before everything else`() {
        val withFavorite = foods + SearchableFood(food("Riz au lait", favorite = true))
        assertEquals("Riz au lait", names("riz", withFavorite).first())
    }

    @Test
    fun `an empty query finds nothing and results are capped`() {
        assertTrue(names("  ").isEmpty())
        val many = (1..100).map { SearchableFood(food("Pomme $it")) }
        assertEquals(SEARCH_RESULT_LIMIT, searchFoods("pomme", many).size)
    }
}
