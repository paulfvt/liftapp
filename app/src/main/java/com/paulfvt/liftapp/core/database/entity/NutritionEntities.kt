package com.paulfvt.liftapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.paulfvt.liftapp.core.model.ActivityLevel
import com.paulfvt.liftapp.core.model.Meal
import com.paulfvt.liftapp.core.model.NutritionObjective
import java.time.Instant
import java.time.LocalDate

// Nutrition (phase 12) : nouvelles tables seulement, les tables existantes ne changent pas.

// Aliment de la table Ciqual (ciqualCode) ou créé par l'utilisateur (ciqualCode nul). Valeurs pour 100 g.
// L'index unique accepte plusieurs ciqualCode nuls : SQLite ne compare pas les NULL entre eux.
@Entity(tableName = "food", indices = [Index(value = ["ciqualCode"], unique = true)])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ciqualCode: Int?,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val isFavorite: Boolean = false,
    // Masqué plutôt que supprimé : les repas déjà notés le gardent (aliment personnel retiré, ou absent d'une
    // nouvelle version de la table).
    val isHidden: Boolean = false
)

// Un aliment noté dans le journal. date : jour du calendrier dans le fuseau du téléphone (un repas appartient à
// une journée, pas à un instant). Les valeurs pour 100 g sont copiées au moment de la saisie : une nouvelle
// version de la table ou un aliment personnel modifié ne changent pas les jours passés.
@Entity(
    tableName = "food_entry",
    foreignKeys = [ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"])],
    indices = [Index("date"), Index("foodId")]
)
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val meal: Meal,
    val foodId: Long,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    // Ordre d'affichage dans le repas.
    val createdAt: Instant
)

// Repas enregistré (« Mes repas ») : une liste d'aliments et de quantités ajoutée d'un coup.
@Entity(tableName = "saved_meal")
data class SavedMealEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(
    tableName = "saved_meal_item",
    foreignKeys = [
        ForeignKey(
            entity = SavedMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedMealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"])
    ],
    indices = [Index("savedMealId"), Index("foodId")]
)
data class SavedMealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savedMealId: Long,
    val foodId: Long,
    val grams: Double,
    val sortOrder: Int
)

// Objectif nutritionnel, historisé comme l'objectif hebdo : il vaut à partir de effectiveFrom, les jours d'avant
// gardent le précédent. Les réglages qui l'ont produit sont gardés pour rouvrir « Mes besoins » tel quel.
@Entity(tableName = "nutrition_goal", indices = [Index(value = ["effectiveFrom"], unique = true)])
data class NutritionGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveFrom: LocalDate,
    val kcal: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val activityLevel: ActivityLevel,
    val objective: NutritionObjective,
    val proteinPerKg: Double,
    val fatPerKg: Double,
    // Vrai si les calories ont été saisies à la main au lieu du calcul.
    val isKcalManual: Boolean
)
