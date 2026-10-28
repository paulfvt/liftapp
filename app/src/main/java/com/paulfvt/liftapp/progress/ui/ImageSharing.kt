package com.paulfvt.liftapp.progress.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SHARE_DIR = "share"
private const val PNG_QUALITY = 100

// Écrit l'image dans le cache de l'app (dossier share/, déclaré dans res/xml/file_paths.xml), puis ouvre le menu de
// partage d'Android. Le FileProvider donne à l'app choisie un accès temporaire à ce seul fichier : rien ne part sans
// que l'utilisateur choisisse lui-même où l'envoyer.
suspend fun shareImage(context: Context, bitmap: Bitmap, fileName: String, chooserTitle: String) {
    val file = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        File(directory, fileName).also { file ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        // ClipData : sans elle, certaines apps ne reçoivent pas le droit de lecture accordé par le drapeau.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
