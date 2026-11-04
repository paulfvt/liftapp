package com.paulfvt.liftapp.profile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.paulfvt.liftapp.profile.domain.sampleSizeFor
import java.io.IOException
import java.io.InputStream

private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f

// Image choisie dans la galerie ou prise par l'appareil photo, réduite à maxSide pixels sur son plus grand côté et
// redressée selon l'orientation notée par l'appareil (photos de progression, étiquettes). À appeler hors du fil
// principal.
fun Context.decodeUpright(uri: Uri, maxSide: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Image illisible : $uri")
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
    }
    val decoded = open(uri).use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IOException("Image illisible : $uri")
    val orientation = open(uri).use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    return decoded.scaledDown(maxSide).rotated(orientation)
}

private fun Context.open(uri: Uri): InputStream =
    contentResolver.openInputStream(uri) ?: throw IOException("Image introuvable : $uri")

private fun Bitmap.scaledDown(maxSide: Int): Bitmap {
    val longest = maxOf(width, height)
    if (longest <= maxSide) return this
    val ratio = maxSide.toFloat() / longest
    return scale((width * ratio).toInt(), (height * ratio).toInt())
}

// Les miroirs, rares, sont ignorés.
private fun Bitmap.rotated(orientation: Int): Bitmap {
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
        ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
        ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
}
