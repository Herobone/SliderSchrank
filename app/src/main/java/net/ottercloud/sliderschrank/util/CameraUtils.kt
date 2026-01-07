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

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CameraUtils"

fun takePictureForPreview(
    context: Context,
    cameraController: LifecycleCameraController,
    scope: CoroutineScope,
    onCaptured: (Bitmap) -> Unit,
    onError: () -> Unit
) {
    cameraController.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                val rotationDegrees = image.imageInfo.rotationDegrees
                image.close()

                scope.launch(Dispatchers.Default) {
                    val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
                    withContext(Dispatchers.Main) {
                        onCaptured(rotatedBitmap)
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                onError()
            }
        }
    )
}

fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat())
    }
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotatedBitmap != bitmap) {
        bitmap.recycle()
    }
    return rotatedBitmap
}

fun saveBitmapToMediaStore(
    context: Context,
    bitmap: Bitmap,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    try {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "SliderSchrank_$timestamp.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SliderSchrank")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (uri != null) {
            val success = context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            } ?: false
            if (success) {
                Log.i(TAG, "Camera saved Image to: $uri")
                onSuccess()
            } else {
                Log.e(TAG, "Failed to compress bitmap")
                context.contentResolver.delete(uri, null, null)
                onError()
            }
        } else {
            Log.e(TAG, "Failed to create MediaStore entry")
            onError()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Image save failed: ${e.message}", e)
        onError()
    }
}

/**
 * Saves both the original bitmap (as JPEG) and a processed bitmap with transparent background (as PNG).
 *
 * @param context The context
 * @param originalBitmap The original captured bitmap
 * @param transparentBitmap The bitmap with transparent background (can be null if processing failed)
 * @param onSuccess Called when both images are saved successfully
 * @param onError Called when an error occurs
 */
fun saveBitmapsWithBackgroundRemoval(
    context: Context,
    originalBitmap: Bitmap,
    transparentBitmap: Bitmap?,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    try {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

        // Save original image as JPEG
        val originalFileName = "SliderSchrank_${timestamp}_original.jpg"
        val originalContentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, originalFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SliderSchrank")
        }

        val originalUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            originalContentValues
        )

        val originalSaved = if (originalUri != null) {
            context.contentResolver.openOutputStream(originalUri)?.use { outputStream ->
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            } ?: false
        } else {
            false
        }

        if (!originalSaved) {
            Log.e(TAG, "Failed to save original image")
            originalUri?.let { context.contentResolver.delete(it, null, null) }
            onError()
            return
        }

        Log.i(TAG, "Saved original image to: $originalUri")

        // Save transparent image as PNG (if available)
        if (transparentBitmap != null) {
            val transparentFileName = "SliderSchrank_${timestamp}_transparent.png"
            val transparentContentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, transparentFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SliderSchrank")
            }

            val transparentUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                transparentContentValues
            )

            val transparentSaved = if (transparentUri != null) {
                context.contentResolver.openOutputStream(transparentUri)?.use { outputStream ->
                    transparentBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                } ?: false
            } else {
                false
            }

            if (!transparentSaved) {
                Log.e(TAG, "Failed to save transparent image")
                transparentUri?.let { context.contentResolver.delete(it, null, null) }
                // Still call onSuccess because original was saved
                onSuccess()
                return
            }

            Log.i(TAG, "Saved transparent image to: $transparentUri")
        } else {
            Log.w(TAG, "No transparent bitmap provided, only original image saved")
        }

        onSuccess()
    } catch (e: Exception) {
        Log.e(TAG, "Image save failed: ${e.message}", e)
        onError()
    }
}

/**
 * Saves a transparent bitmap as PNG to the media store.
 *
 * @param context The context
 * @param bitmap The bitmap with transparent background
 * @param onSuccess Called when the image is saved successfully
 * @param onError Called when an error occurs
 */
fun saveTransparentBitmapToMediaStore(
    context: Context,
    bitmap: Bitmap,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    try {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "SliderSchrank_$timestamp.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SliderSchrank")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (uri != null) {
            val success = context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } ?: false
            if (success) {
                Log.i(TAG, "Saved transparent image to: $uri")
                onSuccess()
            } else {
                Log.e(TAG, "Failed to compress transparent bitmap")
                context.contentResolver.delete(uri, null, null)
                onError()
            }
        } else {
            Log.e(TAG, "Failed to create MediaStore entry for transparent image")
            onError()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Transparent image save failed: ${e.message}", e)
        onError()
    }
}