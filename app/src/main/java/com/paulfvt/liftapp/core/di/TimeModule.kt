package com.paulfvt.liftapp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

// L'heure passe par une Clock injectée plutôt que par Instant.now() : les tests peuvent fixer l'heure.
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
