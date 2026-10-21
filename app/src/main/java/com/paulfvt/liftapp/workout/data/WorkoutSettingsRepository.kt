package com.paulfvt.liftapp.workout.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Réglages de la séance. Son et vibration sont réglés depuis la séance en attendant l'écran Réglages (phase 10).
data class WorkoutSettings(
    val restSound: Boolean = true,
    val restVibration: Boolean = true,
    val gymMode: Boolean = false,
    // Vrai une fois les permissions (notifications, alarmes) demandées : on ne les redemande pas à chaque séance.
    val permissionsAsked: Boolean = false
)

// Les réglages vivent dans le DataStore, pas dans Room : ce sont des préférences, pas des données d'entraînement.
@Singleton
class WorkoutSettingsRepository @Inject constructor(private val preferences: DataStore<Preferences>) {
    val settings: Flow<WorkoutSettings> = preferences.data.map { prefs ->
        WorkoutSettings(
            restSound = prefs[REST_SOUND] ?: true,
            restVibration = prefs[REST_VIBRATION] ?: true,
            gymMode = prefs[GYM_MODE] ?: false,
            permissionsAsked = prefs[PERMISSIONS_ASKED] ?: false
        )
    }.distinctUntilChanged()

    suspend fun current(): WorkoutSettings = settings.first()

    suspend fun setRestSound(enabled: Boolean) = preferences.edit { it[REST_SOUND] = enabled }

    suspend fun setRestVibration(enabled: Boolean) = preferences.edit { it[REST_VIBRATION] = enabled }

    suspend fun setGymMode(enabled: Boolean) = preferences.edit { it[GYM_MODE] = enabled }

    suspend fun markPermissionsAsked() = preferences.edit { it[PERMISSIONS_ASKED] = true }

    private companion object {
        val REST_SOUND = booleanPreferencesKey("rest_sound")
        val REST_VIBRATION = booleanPreferencesKey("rest_vibration")
        val GYM_MODE = booleanPreferencesKey("gym_mode")
        val PERMISSIONS_ASKED = booleanPreferencesKey("workout_permissions_asked")
    }
}
