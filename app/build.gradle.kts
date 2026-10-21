plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.paulfvt.liftapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.paulfvt.liftapp"
        minSdk = 26
        // Niveau exigé par Google Play pour les nouvelles apps depuis août 2026.
        // À revérifier au moment de la publication.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // MigrationTestHelper : ouvre une base dans un ancien schéma pour tester chaque migration.
    androidTestImplementation(libs.androidx.room.testing)
}
