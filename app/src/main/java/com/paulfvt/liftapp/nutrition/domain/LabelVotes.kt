package com.paulfvt.liftapp.nutrition.domain

// Scan en direct (11 novembre 2026) : la caméra lit plusieurs images par seconde et chacune vote pour ses valeurs.
// Une valeur est retenue dès qu'au moins deux des dernières lectures la donnent ; la plus fréquente l'emporte, la plus
// récente en cas d'égalité. Un chiffre mal lu sur une image isolée ne s'affiche donc jamais.
data class LabelVotes(private val readings: List<LabelReading> = emptyList()) {
    // Une image sans aucune valeur ne vote pas : bouger le téléphone n'efface pas ce qui est déjà lu.
    fun add(reading: LabelReading): LabelVotes = if (reading.foundCount == 0 && reading.fiber == null) {
        this
    } else {
        LabelVotes((readings + reading).takeLast(WINDOW))
    }

    val result: LabelReading
        get() = LabelReading(
            kcal = winner { it.kcal },
            protein = winner { it.protein },
            carbs = winner { it.carbs },
            fat = winner { it.fat },
            fiber = winner { it.fiber }
        )

    private fun winner(value: (LabelReading) -> Double?): Double? {
        val seen = readings.mapNotNull(value)
        val counts = seen.groupingBy { it }.eachCount()
        val best = counts.values.maxOrNull()?.takeIf { it >= MIN_AGREEMENT } ?: return null
        return seen.last { counts[it] == best }
    }

    private companion object {
        // Une douzaine de lectures, soit quelques secondes de scan.
        const val WINDOW = 12
        const val MIN_AGREEMENT = 2
    }
}
