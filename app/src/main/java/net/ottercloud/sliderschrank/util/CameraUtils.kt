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
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.graphics.scale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
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
        Executors.newSingleThreadExecutor(),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                val rotationDegrees = image.imageInfo.rotationDegrees
                image.close()

                scope.launch(Dispatchers.Default) {
                    val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
                    val scaledBitmap = scaleBitmapDown(rotatedBitmap, 1080)
                    withContext(Dispatchers.Main) {
                        onCaptured(scaledBitmap)
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

private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    var newWidth: Int
    var newHeight: Int

    if (originalHeight > maxDimension || originalWidth > maxDimension) {
        val ratio = if (originalWidth > originalHeight) {
            maxDimension.toFloat() / originalWidth
        } else {
            maxDimension.toFloat() / originalHeight
        }
        newWidth = (originalWidth * ratio).toInt()
        newHeight = (originalHeight * ratio).toInt()
    } else {
        return bitmap
    }

    val scaled = bitmap.scale(newWidth, newHeight)
    if (scaled != bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
    }
    return scaled
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

/**
 * Internal helper function to save a bitmap to the media store.
 *
 * @param context The context
 * @param bitmap The bitmap to save
 * @param fileName The file name
 * @param mimeType The MIME type (e.g., "image/jpeg" or "image/png")
 * @param format The compression format
 * @param quality The compression quality (0-100)
 * @return The URI of the saved image, or null if saving failed
 */
private fun saveBitmapInternal(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    mimeType: String,
    format: Bitmap.CompressFormat,
    quality: Int
): Uri? {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SliderSchrank")
    }

    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: return null

    val success = context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        bitmap.compress(format, quality, outputStream)
    } ?: false

    if (!success) {
        context.contentResolver.delete(uri, null, null)
        return null
    }

    return uri
}

suspend fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap): Uri? =
    withContext(Dispatchers.IO) {
        try {
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "SliderSchrank_$timestamp.jpg"

            val uri = saveBitmapInternal(
                context,
                bitmap,
                fileName,
                "image/jpeg",
                Bitmap.CompressFormat.JPEG,
                85
            )

            if (uri != null) {
                Log.i(TAG, "Camera saved Image to: $uri")
                uri
            } else {
                Log.e(TAG, "Failed to save bitmap")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image save failed: ${e.message}", e)
            null
        }
    }

/**
 * Saves a transparent bitmap as PNG to the media store.
 *
 * @param context The context
 * @param bitmap The bitmap with transparent background
 * @return The URI of the saved image, or null if saving failed
 */
suspend fun saveTransparentBitmapToMediaStore(context: Context, bitmap: Bitmap): Uri? =
    withContext(Dispatchers.IO) {
        try {
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "SliderSchrank_$timestamp.png"

            val uri = saveBitmapInternal(
                context,
                bitmap,
                fileName,
                "image/png",
                Bitmap.CompressFormat.PNG,
                100
            )

            if (uri != null) {
                Log.i(TAG, "Saved transparent image to: $uri")
                uri
            } else {
                Log.e(TAG, "Failed to save transparent bitmap")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transparent image save failed: ${e.message}", e)
            null
        }
    }