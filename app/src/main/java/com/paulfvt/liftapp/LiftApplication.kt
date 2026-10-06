package com.paulfvt.liftapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Point d'entrée de Hilt : génère le conteneur de dépendances qui vit aussi longtemps que l'app.
@HiltAndroidApp
class LiftApplication : Application()
