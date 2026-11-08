package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.domain.contracts.ProfilePhotoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * SRP (Single Responsibility Principle) & DIP (Dependency Inversion Principle):
 * Encapsulates decoding, square center-cropping, resizing (160x160), and Base64 JPEG
 * compression of student profile photos selected via the zero-permission Android Photo Picker.
 */
class ProfilePhotoProcessor : ProfilePhotoService {

    override suspend fun encodePhotoUriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalArgumentException("Gagal membaca gambar profil."))

                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext Result.failure(IllegalArgumentException("Format gambar tidak didukung."))

                val size = min(rawBitmap.width, rawBitmap.height)
                val xOffset = (rawBitmap.width - size) / 2
                val yOffset = (rawBitmap.height - size) / 2
                val cropped = Bitmap.createBitmap(rawBitmap, xOffset, yOffset, size, size)
                val scaled = Bitmap.createScaledBitmap(cropped, TARGET_AVATAR_PX, TARGET_AVATAR_PX, true)

                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

                if (cropped !== rawBitmap) rawBitmap.recycle()
                if (scaled !== cropped) cropped.recycle()

                Result.success(base64)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val TARGET_AVATAR_PX = 160
        private const val JPEG_QUALITY = 75

        fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? {
            if (base64Str.isBlank()) return null
            return try {
                val clean = base64Str.substringAfter("base64,", base64Str).trim()
                val decoded = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
}
