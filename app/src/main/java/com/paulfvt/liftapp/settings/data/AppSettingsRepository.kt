package com.paulfvt.liftapp.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.paulfvt.liftapp.template.domain.DEFAULT_REST_SECONDS
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

// Réglages de l'app (cahier § 4.11). Son, vibration et mode salle restent dans WorkoutSettingsRepository, l'objectif
// hebdomadaire dans Room (il est historisé).
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Repos donné aux exercices ajoutés à un template ou à une séance.
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS
)

// Des préférences, donc dans le DataStore et pas dans Room.
@Singleton
class AppSettingsRepository @Inject constructor(private val preferences: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = preferences.data.map { prefs ->
        AppSettings(
            themeMode = enumOrDefault(prefs[THEME_MODE], ThemeMode.SYSTEM),
            defaultRestSeconds = prefs[DEFAULT_REST] ?: DEFAULT_REST_SECONDS
        )
    }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = preferences.edit { it[THEME_MODE] = mode.name }

    suspend fun setDefaultRestSeconds(seconds: Int) = preferences.edit { it[DEFAULT_REST] = seconds }

    private companion object {
        val THEME_MODE = stringPreferencesKey("settings_theme_mode")
        val DEFAULT_REST = intPreferencesKey("settings_default_rest_seconds")

        // Une valeur inconnue (enregistrée par une version future) retombe sur la valeur par défaut.
        inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
