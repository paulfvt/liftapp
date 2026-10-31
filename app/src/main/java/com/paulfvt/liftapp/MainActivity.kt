package com.paulfvt.liftapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.paulfvt.liftapp.core.designsystem.theme.LiftTheme
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.navigation.LiftApp
import com.paulfvt.liftapp.profile.data.PhotoLockSession
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import com.paulfvt.liftapp.settings.data.ThemeMode
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

// Activité unique : tous les écrans sont des destinations Compose gérées par la navigation. FragmentActivity (et non
// ComponentActivity) : l'invite de déverrouillage par visage ou empreinte d'androidx.biometric l'exige.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var photoLock: PhotoLockSession

    @Inject
    lateinit var workouts: WorkoutRepository

    @Inject
    lateinit var appSettings: AppSettingsRepository

    // Augmente à chaque appui sur la notification de séance alors que l'app est déjà ouverte.
    private var openWorkoutRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dessine sous les barres système. Le Scaffold de LiftApp gère les marges (insets).
        enableEdgeToEdge()
        setContent {
            // null tant que les réglages ne sont pas lus (quelques millisecondes) : le fond de la fenêtre reste affiché.
            val settings by appSettings.settings.collectAsStateWithLifecycle(initialValue = null)
            val themeMode = settings?.themeMode ?: return@setContent
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Icônes des barres système claires sur fond sombre, et inversement, selon le thème de l'app (pas celui du
            // téléphone quand l'utilisateur en a choisi un).
            DisposableEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            LiftTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalUnits provides (settings?.units ?: Units())) {
                    LiftApp(openWorkoutRequest)
                }
            }
        }
        // Pas après une rotation : l'activité recréée garde l'intention d'origine, déjà traitée.
        if (savedInstanceState == null) handleStartRequest(intent)
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
        handleStartRequest(intent)
    }

    // « Démarrer » du widget : la séance est créée (ou la séance en cours rouverte), puis l'écran de séance s'ouvre.
    // Si le template a été supprimé entre-temps, l'app s'ouvre simplement.
    private fun handleStartRequest(intent: Intent) {
        if (intent.action != ACTION_START_WORKOUT) return
        val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, NO_TEMPLATE).takeIf { it != NO_TEMPLATE }
        val emptyName = getString(R.string.workout_empty_name)
        lifecycleScope.launch {
            if (workouts.startOrResume(templateId, emptyName)) openWorkoutRequest++
        }
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "com.paulfvt.liftapp.action.OPEN_WORKOUT"
        const val ACTION_START_WORKOUT = "com.paulfvt.liftapp.action.START_WORKOUT"
        const val EXTRA_TEMPLATE_ID = "template_id"

        // Séance vide (aucun template).
        const val NO_TEMPLATE = -1L
    }
}
