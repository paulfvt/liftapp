package com.paulfvt.liftapp.profile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.paulfvt.liftapp.profile.domain.Sex
import com.paulfvt.liftapp.profile.domain.UserProfile
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Profil fixe : une préférence par champ dans le DataStore (cahier § 5), pas de table Room.
@Singleton
class ProfileRepository @Inject constructor(private val preferences: DataStore<Preferences>) {
    val profile: Flow<UserProfile> = preferences.data.map { prefs ->
        UserProfile(
            firstName = prefs[FIRST_NAME],
            heightCm = prefs[HEIGHT_CM],
            birthDate = prefs[BIRTH_DATE]?.let(LocalDate::ofEpochDay),
            // Valeur inconnue (version future de l'app, par exemple) : traitée comme non renseignée.
            sex = prefs[SEX]?.let { name -> Sex.entries.firstOrNull { it.name == name } }
        )
    }.distinctUntilChanged()

    suspend fun current(): UserProfile = profile.first()

    suspend fun save(profile: UserProfile) {
        preferences.edit { prefs ->
            prefs.setOrRemove(FIRST_NAME, profile.firstName)
            prefs.setOrRemove(HEIGHT_CM, profile.heightCm)
            prefs.setOrRemove(BIRTH_DATE, profile.birthDate?.toEpochDay())
            prefs.setOrRemove(SEX, profile.sex?.name)
        }
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else set(key, value)
    }

    private companion object {
        val FIRST_NAME = stringPreferencesKey("profile_first_name")
        val HEIGHT_CM = doublePreferencesKey("profile_height_cm")
        val BIRTH_DATE = longPreferencesKey("profile_birth_date")
        val SEX = stringPreferencesKey("profile_sex")
    }
}
