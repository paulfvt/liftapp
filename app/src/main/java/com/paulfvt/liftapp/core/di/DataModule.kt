package com.paulfvt.liftapp.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.dao.ExerciseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Explique à Hilt comment construire la base et le DataStore, en un seul exemplaire pour toute l'app.
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    // Nom du fichier des préférences (files/datastore/lift_preferences.preferences_pb), repris par la sauvegarde.
    const val PREFERENCES_NAME = "lift_preferences"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LiftDatabase = LiftDatabase.build(context)

    @Provides
    fun provideExerciseDao(database: LiftDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(PREFERENCES_NAME) }
}
