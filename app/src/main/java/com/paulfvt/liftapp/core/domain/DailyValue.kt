package com.paulfvt.liftapp.core.domain

import java.time.LocalDate

// Une valeur par jour : c'est ce que dessinent les courbes (progression d'un exercice, mesures du corps).
data class DailyValue(val date: LocalDate, val value: Double)
