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
import android.util.Log
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.nio.FloatBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "BackgroundRemover"

/**
 * Utility class for removing backgrounds from images using ML Kit Subject Segmentation.
 * Optimized for images of clothing items lying flat on monotone backgrounds.
 */
object BackgroundRemover {

    private var segmenter: SubjectSegmenter? = null

    private fun getOrInitSegmenter(): SubjectSegmenter {
        synchronized(this) {
            if (segmenter == null) {
                val options = SubjectSegmenterOptions.Builder()
                    .enableForegroundConfidenceMask()
                    .build()
                segmenter = SubjectSegmentation.getClient(options)
            }
            return segmenter!!
        }
    }

    /**
     * Removes the background from a bitmap, making it transparent.
     *
     * @param bitmap The input bitmap to process
     * @return A new bitmap with transparent background, or null if segmentation fails
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val currentSegmenter = getOrInitSegmenter()
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val result = suspendCancellableCoroutine<SubjectSegmentationResult> { continuation ->
                currentSegmenter.process(inputImage)
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
                return@withContext null
            }

            // The mask has the same dimensions as the input image
            val maskWidth = bitmap.width
            val maskHeight = bitmap.height

            // Create a new bitmap with alpha channel (ARGB_8888)
            val resultBitmap = createBitmap(maskWidth, maskHeight)

            // Rewind the buffer to read from the beginning
            confidenceMask.rewind()

            // Use bulk pixel operations for better performance
            val pixelCount = maskWidth * maskHeight

            // Read confidence mask into array (bulk operation is much faster than per-pixel get())
            val confidences = FloatArray(pixelCount)
            confidenceMask.get(confidences)

            val originalPixels = IntArray(pixelCount)
            val resultPixels = IntArray(pixelCount)

            // Get all pixels at once
            bitmap.getPixels(originalPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

            // Process each pixel
            for (i in 0 until pixelCount) {
                // Get confidence value (0.0 to 1.0)
                val confidence = confidences[i]

                if (confidence > CONFIDENCE_THRESHOLD) {
                    // Foreground (clothing) - keep the pixel with full opacity
                    // Use soft edge based on confidence for smoother edges
                    val alpha = (confidence * 255).toInt().coerceIn(0, 255)
                    val originalPixel = originalPixels[i]

                    resultPixels[i] = (originalPixel and 0x00FFFFFF) or (alpha shl 24)
                } else {
                    // Background - make transparent
                    resultPixels[i] = 0 // Color.TRANSPARENT
                }
            }

            // Set all pixels at once
            resultBitmap.setPixels(resultPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

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
        synchronized(this) {
            val seg = segmenter
            segmenter = null
            if (seg != null) {
                // Close in background to avoid blocking UI thread
                Thread {
                    try {
                        seg.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing segmenter", e)
                    }
                }.start()
            }
        }
    }

    private const val CONFIDENCE_THRESHOLD = 0.5f
}