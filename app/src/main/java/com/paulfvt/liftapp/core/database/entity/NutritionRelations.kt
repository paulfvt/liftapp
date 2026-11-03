package com.paulfvt.liftapp.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import java.time.LocalDate

// Aliment noté avec le nom actuel de l'aliment (un aliment personnel renommé l'est aussi dans les jours passés).
data class FoodEntryRow(@Embedded val entry: FoodEntryEntity, val foodName: String)

// Totaux d'une journée du journal (calories et macros des quantités notées).
data class DailyNutrientsRow(
    val date: LocalDate,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class SavedMealDetails(
    @Embedded val meal: SavedMealEntity,
    @Relation(parentColumn = "id", entityColumn = "savedMealId")
    val items: List<SavedMealItemEntity>
)
