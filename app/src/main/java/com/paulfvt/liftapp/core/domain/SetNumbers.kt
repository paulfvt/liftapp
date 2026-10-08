package com.paulfvt.liftapp.core.domain

import com.paulfvt.liftapp.core.model.SetType

// Numéro affiché de chaque série : les séries de travail sont numérotées 1, 2, 3… dans l'ordre ;
// échauffement et dégressive n'ont pas de numéro (affichées « É » et « D »).
fun workSetNumbers(types: List<SetType>): List<Int?> {
    var number = 0
    return types.map { if (it == SetType.WORK) ++number else null }
}
