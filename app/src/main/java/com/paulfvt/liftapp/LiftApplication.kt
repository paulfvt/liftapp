package com.paulfvt.liftapp

import android.app.Application
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.exercise.data.CatalogImporter
import com.paulfvt.liftapp.workout.notification.WorkoutSessionSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Point d'entrée de Hilt : génère le conteneur de dépendances qui vit aussi longtemps que l'app.
@HiltAndroidApp
class LiftApplication : Application() {
    @Inject lateinit var catalogImporter: CatalogImporter

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject lateinit var workoutSessionSync: WorkoutSessionSync

    override fun onCreate() {
        super.onCreate()
        // En arrière-plan pour ne pas retarder l'ouverture de l'app. Les écrans observent la base
        // et se mettent à jour d'eux-mêmes quand l'import se termine.
        applicationScope.launch { catalogImporter.importIfNeeded() }
        // Notification et alarme de la séance en cours, aussi quand l'app est démarrée par une alarme ou une action.
        workoutSessionSync.start()
    }
}
