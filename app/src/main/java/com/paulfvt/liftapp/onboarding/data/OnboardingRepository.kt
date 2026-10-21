package com.paulfvt.liftapp.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Premier lancement : l'écran de bienvenue a-t-il déjà été passé ? Une préférence, donc dans le DataStore.
@Singleton
class OnboardingRepository @Inject constructor(private val preferences: DataStore<Preferences>) {
    val isWelcomeDone: Flow<Boolean> = preferences.data.map { it[WELCOME_DONE] ?: false }.distinctUntilChanged()

    suspend fun markWelcomeDone() {
        preferences.edit { it[WELCOME_DONE] = true }
    }

    private companion object {
        val WELCOME_DONE = booleanPreferencesKey("welcome_done")
    }
}
