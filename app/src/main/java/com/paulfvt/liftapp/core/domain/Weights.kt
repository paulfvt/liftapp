package com.paulfvt.liftapp.core.domain

import kotlin.math.roundToLong

private const val DISPLAY_STEPS_PER_KG = 2

// Arrondi d'affichage en kilos : au demi-kilo le plus proche (décision du 6 octobre 2026). La valeur enregistrée,
// elle, n'est jamais arrondie.
fun roundForDisplay(kg: Double): Double = (kg * DISPLAY_STEPS_PER_KG).roundToLong().toDouble() / DISPLAY_STEPS_PER_KG
