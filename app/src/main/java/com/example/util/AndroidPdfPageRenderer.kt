package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import com.example.domain.contracts.PdfPageRendererService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * SRP (Single Responsibility Principle) & DIP (Dependency Inversion Principle):
 * Isolates Android PdfRenderer bitmap extraction behind PdfPageRendererService.
 */
class AndroidPdfPageRenderer : PdfPageRendererService {

    override suspend fun renderPages(
        context: Context,
        uri: Uri,
        startPage: Int,
        maxPages: Int
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    if (total <= 0) return@withContext emptyList()
                    val firstIdx = (startPage - 1).coerceIn(0, total - 1)
                    val lastIdx = min(total - 1, firstIdx + maxPages - 1)
                    for (pageIdx in firstIdx..lastIdx) {
                        renderer.openPage(pageIdx).use { page ->
                            val scale = min(1.5f, 1080f / max(1, page.width))
                            val w = max(1, (page.width * scale).roundToInt())
                            val h = max(1, (page.height * scale).roundToInt())
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bmp)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidPdfPageRenderer", "Failed to render PDF pages", e)
        }
        bitmaps
    }
}
