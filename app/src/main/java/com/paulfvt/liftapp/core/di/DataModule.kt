package com.paulfvt.liftapp.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
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
    // Pas de fallbackToDestructiveMigration() : il effacerait les données des utilisateurs.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LiftDatabase =
        Room.databaseBuilder(context, LiftDatabase::class.java, LiftDatabase.NAME).build()

    @Provides
    fun provideExerciseDao(database: LiftDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("lift_preferences") }
}
