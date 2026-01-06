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
                // Sort pieces by slot order (HEAD, TOP, BOTTOM, FEET, ACCESSORY)
                val sortedPieces = pieces.sortedBy { getSlotOrder(it.slot) }

                // Validate non-empty pieces list
                if (sortedPieces.isEmpty()) {
                    Log.w(TAG, "No pieces provided, cannot generate outfit image")
                    return@withContext ""
                }

                // Calculate max height per piece defensively (avoid unreasonably small values)
                val maxHeightPerPiece = maxOf(50, OUTPUT_HEIGHT / sortedPieces.size)

                // Load and scale all bitmaps first
                val scaledBitmaps = sortedPieces.mapNotNull { piece ->
                    val bitmap = loadBitmapFromUri(context, piece.imageUrl)
                    if (bitmap != null) {
                        val scaledBitmap = scaleBitmapToFit(
                            bitmap,
                            OUTPUT_WIDTH,
                            maxHeightPerPiece
                        )
                        if (scaledBitmap != bitmap) {
                            bitmap.recycle()
                        }

                        // HEAD and FEET scale to 2/3 of size
                        val finalBitmap = if (piece.slot == Slot.HEAD || piece.slot == Slot.FEET) {
                            val smallerWidth = (scaledBitmap.width * 2 / 3)
                            val smallerHeight = (scaledBitmap.height * 2 / 3)
                            val smaller = scaledBitmap.scale(smallerWidth, smallerHeight)
                            scaledBitmap.recycle()
                            smaller
                        } else {
                            scaledBitmap
                        }

                        finalBitmap
                    } else {
                        Log.w(TAG, "Failed to load bitmap for piece: ${piece.imageUrl}")
                        null
                    }
                }

                if (scaledBitmaps.isEmpty()) {
                    Log.w(TAG, "No bitmaps loaded, cannot generate outfit image")
                    return@withContext ""
                }

                // Calculate total height needed
                val totalHeight = scaledBitmaps.sumOf { it.height }

                // Create a bitmap with transparent background
                val compositeBitmap = createBitmap(OUTPUT_WIDTH, totalHeight)
                val canvas = Canvas(compositeBitmap)
                // Note: Canvas is transparent by default, no need to clear

                // Draw each piece vertically, one above the other
                var currentY = 0f
                for (bitmap in scaledBitmaps) {
                    // Center the bitmap horizontally
                    val left = (OUTPUT_WIDTH - bitmap.width) / 2f
                    canvas.drawBitmap(bitmap, left, currentY, null)
                    currentY += bitmap.height
                    bitmap.recycle()
                }

                // Crop the image to remove excess transparent space (10px padding)
                val croppedBitmap = cropToContent(compositeBitmap, padding = 10)
                compositeBitmap.recycle()

                // Save the cropped bitmap to internal storage
                val savedUri = saveBitmapToInternalStorage(context, croppedBitmap)
                croppedBitmap.recycle()

                savedUri
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate outfit image", e)
                ""
            }
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

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        // Find the bounds of non-transparent pixels by scanning from edges inward
        var contentFound = false
        // Scan from top to find first row with content
        topScan@ for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap[x, y]
                val alpha = (pixel shr 24) and 0xff
                if (alpha > 0) {
                    minY = y
                    contentFound = true
                    break@topScan
                }
            }
        }
        // If nothing found yet, there's no content at all
        if (!contentFound) {
            return bitmap
        }
        // Scan from bottom to find last row with content
        bottomScan@ for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                val pixel = bitmap[x, y]
                val alpha = (pixel shr 24) and 0xff
                if (alpha > 0) {
                    maxY = y
                    break@bottomScan
                }
            }
        }
        // Scan from left to find first column with content within the vertical band
        leftScan@ for (x in 0 until width) {
            for (y in minY..maxY) {
                val pixel = bitmap[x, y]
                val alpha = (pixel shr 24) and 0xff
                if (alpha > 0) {
                    minX = x
                    break@leftScan
                }
            }
        }
        // Scan from right to find last column with content within the vertical band
        rightScan@ for (x in width - 1 downTo 0) {
            for (y in minY..maxY) {
                val pixel = bitmap[x, y]
                val alpha = (pixel shr 24) and 0xff
                if (alpha > 0) {
                    maxX = x
                    break@rightScan
                }
            }
        }
        // If no valid content bounds found, return original bitmap
        if (minX > maxX || minY > maxY) {
            return bitmap
        }

        // Add padding and clamp to bitmap bounds
        minX = maxOf(0, minX - padding)
        minY = maxOf(0, minY - padding)
        maxX = minOf(width - 1, maxX + padding)
        maxY = minOf(height - 1, maxY + padding)

        val croppedWidth = maxX - minX + 1
        val croppedHeight = maxY - minY + 1

        return Bitmap.createBitmap(bitmap, minX, minY, croppedWidth, croppedHeight)
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