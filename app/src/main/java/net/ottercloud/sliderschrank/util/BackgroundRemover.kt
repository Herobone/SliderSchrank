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

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.nio.FloatBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "BackgroundRemover"

/**
 * Utility class for removing backgrounds from images using ML Kit Subject Segmentation.
 * Optimized for images of clothing items lying flat on monotone backgrounds.
 */
object BackgroundRemover {

    private val segmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .build()
        SubjectSegmentation.getClient(options)
    }

    /**
     * Removes the background from a bitmap, making it transparent.
     *
     * @param bitmap The input bitmap to process
     * @return A new bitmap with transparent background, or null if segmentation fails
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap? {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val result = suspendCancellableCoroutine<SubjectSegmentationResult> { continuation ->
                segmenter.process(inputImage)
                    .addOnSuccessListener { segmentationResult ->
                        continuation.resume(segmentationResult)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Segmentation failed", exception)
                        continuation.resumeWithException(exception)
                    }
            }

            val confidenceMask: FloatBuffer? = result.foregroundConfidenceMask
            if (confidenceMask == null) {
                Log.e(TAG, "No foreground confidence mask available")
                return null
            }

            // The mask has the same dimensions as the input image
            val maskWidth = bitmap.width
            val maskHeight = bitmap.height

            // Create a new bitmap with alpha channel (ARGB_8888)
            val resultBitmap = Bitmap.createBitmap(
                maskWidth,
                maskHeight,
                Bitmap.Config.ARGB_8888
            )

            // Rewind the buffer to read from the beginning
            confidenceMask.rewind()

            // Process each pixel
            for (y in 0 until maskHeight) {
                for (x in 0 until maskWidth) {
                    // Get confidence value (0.0 to 1.0)
                    val confidence = confidenceMask.get()

                    val originalPixel = bitmap.getPixel(x, y)

                    if (confidence > CONFIDENCE_THRESHOLD) {
                        // Foreground (clothing) - keep the pixel with full opacity
                        // Use soft edge based on confidence for smoother edges
                        val alpha = (confidence * 255).toInt().coerceIn(0, 255)
                        val newPixel = Color.argb(
                            alpha,
                            Color.red(originalPixel),
                            Color.green(originalPixel),
                            Color.blue(originalPixel)
                        )
                        resultBitmap.setPixel(x, y, newPixel)
                    } else {
                        // Background - make transparent
                        resultBitmap.setPixel(x, y, Color.TRANSPARENT)
                    }
                }
            }

            Log.i(TAG, "Background removal completed successfully")
            resultBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error removing background", e)
            null
        }
    }

    /**
     * Closes the segmenter to release resources.
     * Call this when background removal is no longer needed.
     */
    fun close() {
        segmenter.close()
    }

    private const val CONFIDENCE_THRESHOLD = 0.5f
}