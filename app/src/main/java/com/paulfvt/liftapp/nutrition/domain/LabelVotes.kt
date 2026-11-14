package com.paulfvt.liftapp.nutrition.domain

import kotlin.math.abs

// Scan en direct (11 novembre 2026) : la caméra lit plusieurs images par seconde et chacune vote pour ses valeurs.
// Une valeur est retenue dès qu'au moins deux des dernières lectures la donnent ; la plus fréquente l'emporte, la plus
// récente en cas d'égalité. Un chiffre mal lu sur une image isolée ne s'affiche donc jamais.
// La virgule d'une macro se perd sur certaines images (« 5,8 » lu « 58 », 12 novembre 2026) : les deux lectures
// votent ensemble, puis les calories tranchent ; sans elles, la version à virgule gagne, car la lecture perd souvent
// une virgule mais n'en invente presque jamais.
data class LabelVotes(private val readings: List<LabelReading> = emptyList()) {
    // Une image sans aucune valeur ne vote pas : bouger le téléphone n'efface pas ce qui est déjà lu.
    fun add(reading: LabelReading): LabelVotes = if (reading.foundCount == 0 && reading.fiber == null) {
        this
    } else {
        LabelVotes((readings + reading).takeLast(WINDOW))
    }

    val result: LabelReading
        get() {
            val kcal = winner(candidates(withTenTimes = false) { it.kcal })?.value
            val fiber = winner(candidates { it.fiber })?.value
            val (protein, carbs, fat) = macros(
                winner(candidates { it.protein }),
                winner(candidates { it.carbs }),
                winner(candidates { it.fat }),
                kcal,
                fiber ?: 0.0
            )
            return LabelReading(kcal = kcal, protein = protein, carbs = carbs, fat = fat, fiber = fiber)
        }

    // Valeurs lues pour un champ, avec leurs voix. withTenTimes : une valeur et sa version dix fois plus grande
    // forment un seul candidat.
    private fun candidates(withTenTimes: Boolean = true, value: (LabelReading) -> Double?): List<Candidate> {
        val seen = readings.mapIndexedNotNull { index, reading -> value(reading)?.let { it to index } }
        val votes = seen.groupingBy { it.first }.eachCount()
        val lastSeen = seen.associate { it }
        val paired = mutableSetOf<Double>()
        return votes.keys.sorted().mapNotNull { small ->
            if (small in paired) return@mapNotNull null
            val big = if (withTenTimes) votes.keys.firstOrNull { abs(it - small * TEN) < EPSILON } else null
            big?.let(paired::add)
            Candidate(
                value = small,
                tenTimes = big,
                votes = votes.getValue(small) + (big?.let(votes::getValue) ?: 0),
                lastSeen = maxOf(lastSeen.getValue(small), big?.let(lastSeen::getValue) ?: -1)
            )
        }
    }

    private fun winner(candidates: List<Candidate>): Candidate? = candidates
        .filter { it.votes >= MIN_AGREEMENT }
        .maxWithOrNull(compareBy({ it.votes }, { it.lastSeen }))

    // Choisit, pour chaque macro lue des deux façons, la version qui fait coller les calories ; si aucune combinaison ne
    // colle ou s'il manque une valeur, la version à virgule.
    private fun macros(
        protein: Candidate?,
        carbs: Candidate?,
        fat: Candidate?,
        kcal: Double?,
        fiber: Double
    ): Triple<Double?, Double?, Double?> {
        val default = Triple(protein?.value, carbs?.value, fat?.value)
        if (kcal == null || protein == null || carbs == null || fat == null) return default
        val combinations = protein.options.flatMap { p ->
            carbs.options.flatMap { c -> fat.options.map { f -> Triple(p, c, f) } }
        }
        return combinations
            .filter { (p, c, f) -> isEnergyConsistent(kcal, macrosKcal(p, c, f, fiber)) }
            .minByOrNull { (p, c, f) -> abs(kcal - macrosKcal(p, c, f, fiber)) }
            ?: default
    }

    // value : la plus petite des deux lectures (avec sa virgule), tenTimes : la même sans virgule, si elle a été lue.
    private class Candidate(val value: Double, val tenTimes: Double?, val votes: Int, val lastSeen: Int) {
        val options: List<Double> get() = listOfNotNull(value, tenTimes)
    }

    private companion object {
        // Une douzaine de lectures, soit quelques secondes de scan.
        const val WINDOW = 12
        const val MIN_AGREEMENT = 2
        const val TEN = 10.0
        const val EPSILON = 1e-6
    }
}
