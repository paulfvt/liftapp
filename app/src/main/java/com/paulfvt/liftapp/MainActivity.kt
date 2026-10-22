package com.paulfvt.liftapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.paulfvt.liftapp.core.designsystem.theme.LiftTheme
import com.paulfvt.liftapp.navigation.LiftApp
import com.paulfvt.liftapp.profile.data.PhotoLockSession
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Activité unique : tous les écrans sont des destinations Compose gérées par la navigation. FragmentActivity (et non
// ComponentActivity) : l'invite de déverrouillage par visage ou empreinte d'androidx.biometric l'exige.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var photoLock: PhotoLockSession

    // Augmente à chaque appui sur la notification de séance alors que l'app est déjà ouverte.
    private var openWorkoutRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dessine sous les barres système. Le Scaffold de LiftApp gère les marges (insets).
        enableEdgeToEdge()
        setContent {
            LiftTheme {
                LiftApp(openWorkoutRequest)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        photoLock.onAppResumed()
    }

    // L'app n'est plus visible : les photos se reverrouillent. Une rotation de l'écran recrée l'activité sans
    // quitter l'app, elle ne compte pas.
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) photoLock.onAppStopped()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_WORKOUT) openWorkoutRequest++
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "com.paulfvt.liftapp.action.OPEN_WORKOUT"
    }
}
