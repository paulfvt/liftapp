package com.paulfvt.liftapp.core.model

// Les 4 repas fixes du journal (décision du 3 novembre 2026), dans l'ordre de la journée.
enum class Meal {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACKS
}

// Niveaux d'activité et leur facteur appliqué au métabolisme de base.
enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    ACTIVE(1.55),
    VERY_ACTIVE(1.725),
    EXTREME(1.9)
}

// Objectif de poids et son écart quotidien avec la dépense (kcal).
enum class NutritionObjective(val kcalOffset: Int) {
    CUT(-500),
    MAINTAIN(0),
    BULK(300)
}
