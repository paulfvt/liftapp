import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

// Clé de la version publique (APK des Releases GitHub) : chemin et mots de passe lus dans keystore.properties, ignoré
// par Git (modèle : keystore.properties.example). Toutes les versions doivent être signées avec la même clé : sans
// elle, une mise à jour ne s'installe plus par-dessus la précédente.
val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.paulfvt.liftapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.paulfvt.liftapp"
        minSdk = 26
        targetSdk = 36
        // À augmenter à chaque version publiée : Android refuse une mise à jour dont le versionCode n'est pas plus grand.
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Sans keystore.properties (CI, autre PC), la release est signée avec la clé de debug pour pouvoir
            // l'installer et la tester. Un tel APK ne doit pas être publié : il ne s'installerait pas par-dessus.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            // Retire aussi les ressources inutilisées (images, textes) de l'APK final.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Génère BuildConfig.VERSION_NAME, affiché dans « À propos ».
        buildConfig = true
    }
}

room {
    // Schémas exportés et versionnés dans Git : indispensables pour écrire et tester les migrations.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    // Glisser-déposer dans les listes Compose (Apache 2.0) : Compose ne le propose pas en natif.
    implementation(libs.reorderable)
    // Courbes de progression (Apache 2.0), validé le 21 octobre 2026. Le module Material 3 de Vico n'est pas
    // utilisé : son seul rôle (reprendre les couleurs du thème) tient en quelques lignes dans ProgressChart.
    implementation(libs.vico.compose)
    // Prise de photo dans l'app avec la silhouette fantôme (Apache 2.0), validé le 21 octobre 2026 : l'appareil photo
    // du système ne permet pas de superposer une image. camera-compose affiche l'aperçu directement en Compose.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    // Lecture de l'orientation et de la date des photos. Déjà apportée par CameraX, déclarée car utilisée
    // directement : elle corrige des failles de la version intégrée aux anciens Android.
    implementation(libs.androidx.exifinterface)
    // Déverrouillage des photos par le visage, l'empreinte ou le verrouillage du téléphone (Apache 2.0), validé le
    // 21 octobre 2026. Dernière version stable : 1.1.0.
    implementation(libs.androidx.biometric)
    // Widget d'écran d'accueil (Apache 2.0), validé le 28 octobre 2026 : un widget est dessiné par le lanceur, pas par
    // l'app ; Glance l'écrit comme du Compose puis le traduit en RemoteViews. glance-material3 reprend les couleurs du
    // thème ON AIR, en clair et en sombre.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // MigrationTestHelper : ouvre une base dans un ancien schéma pour tester chaque migration.
    androidTestImplementation(libs.androidx.room.testing)
}
