package com.paulfvt.liftapp.profile.domain

import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.displayValue
import com.paulfvt.liftapp.core.domain.toCm
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.parseDecimal
import java.time.LocalDate
import java.time.Period

enum class Sex {
    FEMALE,
    MALE
}

// Profil fixe : tout est facultatif. La taille est en centimètres (choix cm/in en phase 10).
data class UserProfile(
    val firstName: String? = null,
    val heightCm: Double? = null,
    val birthDate: LocalDate? = null,
    val sex: Sex? = null
)

const val FIRST_NAME_MAX_LENGTH = 40

// Bornes larges : elles n'écartent que les fautes de frappe (« 1800 » au lieu de « 180 »).
const val MIN_HEIGHT_CM = 50.0
const val MAX_HEIGHT_CM = 260.0

// Âge révolu à une date donnée : on ne prend une année qu'au jour de l'anniversaire.
fun ageOn(birthDate: LocalDate, today: LocalDate): Int = Period.between(birthDate, today).years

// Profil en cours de modification : la taille reste le texte tapé jusqu'à l'enregistrement.
data class ProfileDraft(
    val firstName: String = "",
    val height: String = "",
    val birthDate: LocalDate? = null,
    val sex: Sex? = null
)

enum class ProfileError {
    FIRST_NAME_TOO_LONG,
    HEIGHT_INVALID,
    BIRTH_DATE_IN_FUTURE
}

// La taille se saisit en centimètres ou en pouces selon les réglages ; elle est enregistrée en centimètres.
fun UserProfile.toDraft(unit: LengthUnit = LengthUnit.CM) = ProfileDraft(
    firstName = firstName.orEmpty(),
    height = heightCm?.let { formatDecimal(unit.displayValue(it)) }.orEmpty(),
    birthDate = birthDate,
    sex = sex
)

fun ProfileDraft.errors(today: LocalDate, unit: LengthUnit = LengthUnit.CM): Set<ProfileError> = buildSet {
    if (firstName.trim().length > FIRST_NAME_MAX_LENGTH) add(ProfileError.FIRST_NAME_TOO_LONG)
    if (height.isNotBlank()) {
        val cm = parseDecimal(height)?.let { unit.toCm(it) }
        if (cm == null || cm < MIN_HEIGHT_CM || cm > MAX_HEIGHT_CM) add(ProfileError.HEIGHT_INVALID)
    }
    if (birthDate != null && birthDate.isAfter(today)) add(ProfileError.BIRTH_DATE_IN_FUTURE)
}

// À n'appeler que sur un brouillon sans erreur. Un champ vide redevient « non renseigné ».
fun ProfileDraft.toProfile(unit: LengthUnit = LengthUnit.CM) = UserProfile(
    firstName = firstName.trim().ifEmpty { null },
    heightCm = height.takeIf { it.isNotBlank() }?.let(::parseDecimal)?.let { unit.toCm(it) },
    birthDate = birthDate,
    sex = sex
)
