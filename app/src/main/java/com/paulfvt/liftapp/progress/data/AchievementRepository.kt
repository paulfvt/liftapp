package com.paulfvt.liftapp.progress.data

import com.paulfvt.liftapp.progress.domain.Badge
import javax.inject.Inject
import javax.inject.Singleton

// Ce qu'une vérification vient de débloquer.
data class Achievements(val tiers: List<ReachedTier> = emptyList(), val badges: List<Badge> = emptyList())

// Paliers d'objectifs et badges vérifiés d'un coup, après tout ce qui change l'historique : fin de séance (résultat montré
// dans le résumé), séance modifiée ou supprimée, et lancement de l'app (rattrapage silencieux).
@Singleton
class AchievementRepository @Inject constructor(
    private val goals: GoalRepository,
    private val badges: BadgeRepository
) {
    suspend fun refresh(): Achievements = Achievements(tiers = goals.refreshTiers(), badges = badges.refresh())
}
