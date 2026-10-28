package com.paulfvt.liftapp.profile.data

import android.content.Context
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.paulfvt.liftapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Licences dont le texte complet doit accompagner l'app. L'identifiant sert d'argument de navigation.
enum class OpenLicense(val id: String, @param:StringRes val titleRes: Int, @param:RawRes val textRes: Int) {
    APACHE_2_0("apache-2.0", R.string.license_apache_title, R.raw.license_apache_2_0),
    OFL_1_1("ofl-1.1", R.string.license_ofl_title, R.raw.license_ofl_1_1),
    MIT_MUSCLEMAP("mit-musclemap", R.string.license_mit_title, R.raw.license_mit_musclemap);

    companion object {
        fun fromId(id: String): OpenLicense = entries.first { it.id == id }
    }
}

// Textes des licences, rangés dans res/raw avec un paragraphe par ligne pour se replier à la largeur de l'écran.
class LicenseRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    suspend fun text(license: OpenLicense): String = withContext(Dispatchers.IO) {
        context.resources.openRawResource(license.textRes).bufferedReader().use { it.readText() }
    }
}
