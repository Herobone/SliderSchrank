/*
 * Copyright (c) 2025 OtterCloud
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.ottercloud.sliderschrank.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ottercloud.sliderschrank.data.model.Piece
import net.ottercloud.sliderschrank.data.model.Slot

private const val TAG = "OutfitImageGenerator"
private const val OUTPUT_WIDTH = 1080
private const val OUTPUT_HEIGHT = 500 // Max height per piece for scaling

/**
 * Utility class to generate composite images from outfit pieces.
 * Pieces are arranged vertically (HEAD at top, then TOP, BOTTOM, FEET).
 * Output has transparent background and is saved as PNG.
 */
object OutfitImageGenerator {

    /**
     * Generate a composite image from the given pieces and save it to internal storage
     * @param context Application context
     * @param pieces List of clothing pieces to composite
     * @return URI string of the saved image, or empty string if generation fails
     */
    suspend fun generateOutfitImage(context: Context, pieces: List<Piece>): String =
        withContext(Dispatchers.IO) {
            try {
                val sortedPieces = pieces.sortedBy { getSlotOrder(it.slot) }
                if (sortedPieces.isEmpty()) {
                    Log.w(TAG, "No pieces provided, cannot generate outfit image")
                    return@withContext ""
                }

                val scaledBitmaps = loadAndScaleAllBitmaps(context, sortedPieces)
                if (scaledBitmaps.isEmpty()) {
                    Log.w(TAG, "No bitmaps loaded, cannot generate outfit image")
                    return@withContext ""
                }

                val compositeBitmap = createCompositeBitmap(scaledBitmaps)
                val croppedBitmap = cropToContent(compositeBitmap, padding = 10)
                compositeBitmap.recycle()

                val savedUri = saveBitmapToInternalStorage(context, croppedBitmap)
                croppedBitmap.recycle()

                savedUri
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate outfit image", e)
                ""
            }
        }

    /**
     * Load and scale all bitmaps for the given pieces
     */
    private fun loadAndScaleAllBitmaps(context: Context, sortedPieces: List<Piece>): List<Bitmap> {
        val maxHeightPerPiece = maxOf(50, OUTPUT_HEIGHT / sortedPieces.size)
        return sortedPieces.mapNotNull { piece ->
            val bitmap = loadBitmapFromUri(context, piece.imageUrl) ?: return@mapNotNull null
            val scaledBitmap = scaleBitmapToFit(bitmap, OUTPUT_WIDTH, maxHeightPerPiece)
            if (scaledBitmap != bitmap) bitmap.recycle()
            applySlotSpecificScaling(scaledBitmap, piece.slot)
        }
    }

    /**
     * Apply slot-specific scaling (HEAD and FEET scale to 2/3 of size)
     */
    private fun applySlotSpecificScaling(bitmap: Bitmap, slot: Slot): Bitmap =
        if (slot == Slot.HEAD || slot == Slot.FEET) {
            val smallerWidth = bitmap.width * 2 / 3
            val smallerHeight = bitmap.height * 2 / 3
            val smaller = bitmap.scale(smallerWidth, smallerHeight)
            bitmap.recycle()
            smaller
        } else {
            bitmap
        }

    /**
     * Create a composite bitmap by drawing all bitmaps vertically
     */
    private fun createCompositeBitmap(scaledBitmaps: List<Bitmap>): Bitmap {
        val totalHeight = scaledBitmaps.sumOf { it.height }
        val compositeBitmap = createBitmap(OUTPUT_WIDTH, totalHeight)
        val canvas = Canvas(compositeBitmap)

        var currentY = 0f
        for (bitmap in scaledBitmaps) {
            val left = (OUTPUT_WIDTH - bitmap.width) / 2f
            canvas.drawBitmap(bitmap, left, currentY, null)
            currentY += bitmap.height
            bitmap.recycle()
        }

        return compositeBitmap
    }

    /**
     * Load a bitmap from a URI string
     */
    private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? = try {
        val uri = uriString.toUri()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load bitmap from URI: $uriString", e)
        null
    }

    /**
     * Scale a bitmap to fit within the given dimensions while maintaining aspect ratio
     */
    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratioWidth = maxWidth.toFloat() / width
        val ratioHeight = maxHeight.toFloat() / height
        val ratio = minOf(ratioWidth, ratioHeight)

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    /**
     * Crop bitmap to content bounds with specified padding.
     * Removes transparent pixels and keeps only the specified padding around colored pixels.
     */
    private fun cropToContent(bitmap: Bitmap, padding: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val minY = findTopContentEdge(bitmap, width, height)
        if (minY == height) return bitmap // No content found

        val maxY = findBottomContentEdge(bitmap, width, height)
        val minX = findLeftContentEdge(bitmap, width, minY, maxY)
        val maxX = findRightContentEdge(bitmap, width, minY, maxY)

        if (minX > maxX || minY > maxY) return bitmap // No valid bounds

        return createCroppedBitmap(bitmap, width, height, minX, minY, maxX, maxY, padding)
    }

    /**
     * Scan from top to find first row with non-transparent content
     */
    private fun findTopContentEdge(bitmap: Bitmap, width: Int, height: Int): Int {
        for (y in 0 until height) {
            if (hasPixelWithAlphaInRow(bitmap, 0, width - 1, y)) {
                return y
            }
        }
        return height
    }

    /**
     * Scan from bottom to find last row with non-transparent content
     */
    private fun findBottomContentEdge(bitmap: Bitmap, width: Int, height: Int): Int {
        for (y in height - 1 downTo 0) {
            if (hasPixelWithAlphaInRow(bitmap, 0, width - 1, y)) {
                return y
            }
        }
        return -1
    }

    /**
     * Scan from left to find first column with non-transparent content
     */
    private fun findLeftContentEdge(bitmap: Bitmap, width: Int, minY: Int, maxY: Int): Int {
        for (x in 0 until width) {
            if (hasPixelWithAlphaInRegion(bitmap, x, x, minY, maxY)) {
                return x
            }
        }
        return width
    }

    /**
     * Scan from right to find last column with non-transparent content
     */
    private fun findRightContentEdge(bitmap: Bitmap, width: Int, minY: Int, maxY: Int): Int {
        for (x in width - 1 downTo 0) {
            if (hasPixelWithAlphaInRegion(bitmap, x, x, minY, maxY)) {
                return x
            }
        }
        return -1
    }

    /**
     * Check if any pixel in the range [x1, x2] at row y has alpha > 0
     */
    private fun hasPixelWithAlphaInRow(bitmap: Bitmap, x1: Int, x2: Int, y: Int): Boolean {
        for (x in x1..x2) {
            if (getPixelAlpha(bitmap[x, y]) > 0) return true
        }
        return false
    }

    /**
     * Check if any pixel in column [x1, x2] with y in [y1, y2] has alpha > 0
     */
    private fun hasPixelWithAlphaInRegion(
        bitmap: Bitmap,
        x1: Int,
        x2: Int,
        y1: Int,
        y2: Int
    ): Boolean {
        for (x in x1..x2) {
            for (y in y1..y2) {
                if (getPixelAlpha(bitmap[x, y]) > 0) return true
            }
        }
        return false
    }

    /**
     * Extract alpha channel from pixel value
     */
    private fun getPixelAlpha(pixel: Int): Int = (pixel shr 24) and 0xff

    /**
     * Create cropped bitmap with padding applied and clamped to bounds
     */
    private fun createCroppedBitmap(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        padding: Int
    ): Bitmap {
        val clampedMinX = maxOf(0, minX - padding)
        val clampedMinY = maxOf(0, minY - padding)
        val clampedMaxX = minOf(width - 1, maxX + padding)
        val clampedMaxY = minOf(height - 1, maxY + padding)

        val croppedWidth = clampedMaxX - clampedMinX + 1
        val croppedHeight = clampedMaxY - clampedMinY + 1

        return Bitmap.createBitmap(bitmap, clampedMinX, clampedMinY, croppedWidth, croppedHeight)
    }

    /**
     * Save a bitmap to internal storage and return the URI
     */
    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "outfit_$timestamp.png"

        val directory = File(context.filesDir, "outfits")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        Log.d(TAG, "Saved outfit image to: ${file.absolutePath}")
        return Uri.fromFile(file).toString()
    }

    /**
     * Get the order for arranging slots vertically (lower number = drawn at top)
     */
    private fun getSlotOrder(slot: Slot): Int = when (slot) {
        Slot.HEAD -> 0
        Slot.TOP -> 1
        Slot.BOTTOM -> 2
        Slot.FEET -> 3
        Slot.ACCESSORY -> 4
    }
}