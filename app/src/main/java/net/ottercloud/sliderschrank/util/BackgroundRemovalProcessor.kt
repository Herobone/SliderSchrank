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
import android.graphics.Color
import android.util.Log
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "BackgroundRemovalProcessor"

class BackgroundRemovalProcessor private constructor() {

    companion object {
        @Volatile
        private var instance: BackgroundRemovalProcessor? = null

        fun getInstance(): BackgroundRemovalProcessor = instance ?: synchronized(this) {
            instance ?: BackgroundRemovalProcessor().also { instance = it }
        }
    }

    /**
     * Initialisiert den Processor (vereinfachte Version ohne OpenCV)
     */
    suspend fun initializeOpenCV(context: Context): Boolean = withContext(Dispatchers.IO) {
        // Vereinfachte Implementierung - immer erfolgreich
        Log.i(TAG, "Background removal processor initialized (simplified version)")
        return@withContext true
    }

    /**
     * Entfernt den Hintergrund basierend auf Farbschwellwerten
     * Diese Implementierung erkennt helle Hintergründe und macht sie transparent
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        return@withContext try {
            removeBackgroundSimple(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove background: ${e.message}", e)
            null
        }
    }

    /**
     * Einfache Hintergrundentfernung basierend auf Farbschwellwerten
     */
    suspend fun removeBackgroundSimple(
        bitmap: Bitmap,
        backgroundColor: Int = Color.WHITE,
        threshold: Int = 50
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bgR = Color.red(backgroundColor)
        val bgG = Color.green(backgroundColor)
        val bgB = Color.blue(backgroundColor)

        // Erweiterte Hintergrunderkennung mit mehreren Methoden
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // Methode 1: Farbähnlichkeit
            val colorDistance = sqrt(
                ((r - bgR) * (r - bgR) + (g - bgG) * (g - bgG) + (b - bgB) * (b - bgB)).toDouble()
            )

            // Methode 2: Helligkeit (für helle Hintergründe)
            val brightness = (r + g + b) / 3.0

            // Methode 3: Kantenerkennung (vereinfacht)
            val isEdgePixel = isNearEdge(i, width, height, pixels, threshold)

            // Pixel transparent machen wenn es wahrscheinlich Hintergrund ist
            if (colorDistance < threshold || brightness > 200 || !isEdgePixel) {
                // Zusätzlich prüfen ob es nicht ein wichtiges Detail ist
                if (!isImportantDetail(r, g, b)) {
                    pixels[i] = Color.TRANSPARENT
                }
            }
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        resultBitmap
    }

    /**
     * Prüft, ob ein Pixel in der Nähe einer Kante liegt (vereinfachte Kantenerkennung)
     */
    private fun isNearEdge(
        index: Int,
        width: Int,
        height: Int,
        pixels: IntArray,
        threshold: Int
    ): Boolean {
        val x = index % width
        val y = index / width

        // Randpixel überspringen
        if (x < 1 || x >= width - 1 || y < 1 || y >= height - 1) return false

        val currentPixel = pixels[index]
        val currentR = Color.red(currentPixel)
        val currentG = Color.green(currentPixel)
        val currentB = Color.blue(currentPixel)

        // Nachbarpixel prüfen
        val neighbors = listOf(-1, 1, -width, width)

        for (offset in neighbors) {
            val neighborIndex = index + offset
            if (neighborIndex >= 0 && neighborIndex < pixels.size) {
                val neighborPixel = pixels[neighborIndex]
                val neighborR = Color.red(neighborPixel)
                val neighborG = Color.green(neighborPixel)
                val neighborB = Color.blue(neighborPixel)

                val difference = kotlin.math.abs(currentR - neighborR) +
                    kotlin.math.abs(currentG - neighborG) +
                    kotlin.math.abs(currentB - neighborB)

                if (difference > threshold) {
                    return true // Es ist eine Kante
                }
            }
        }

        return false
    }

    /**
     * Prüft, ob ein Pixel ein wichtiges Detail darstellt (z.B. dunkle Farben, die typisch für Kleidung sind)
     */
    private fun isImportantDetail(r: Int, g: Int, b: Int): Boolean {
        val brightness = (r + g + b) / 3.0

        // Dunkle Pixel sind wahrscheinlich wichtig
        if (brightness < 100) return true

        // Gesättigte Farben sind wahrscheinlich wichtig
        val maxColor = maxOf(r, g, b)
        val minColor = minOf(r, g, b)
        val saturation = if (maxColor != 0) (maxColor - minColor).toDouble() / maxColor else 0.0

        return saturation > 0.3
    }
}