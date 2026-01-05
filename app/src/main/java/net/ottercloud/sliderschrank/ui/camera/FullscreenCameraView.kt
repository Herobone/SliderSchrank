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
package net.ottercloud.sliderschrank.ui.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.ottercloud.sliderschrank.util.saveBitmapToMediaStore
import net.ottercloud.sliderschrank.util.takePictureForPreview

private const val TAG = "FullscreenCameraView"

@Composable
fun FullscreenCameraView(
    onClose: () -> Unit,
    onSaveError: () -> Unit,
    onCameraInitError: () -> Unit,
    onCaptureError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Use rememberUpdatedState to ensure LaunchedEffect always has the latest callback reference
    val currentOnCameraInitError by rememberUpdatedState(onCameraInitError)

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Cleanup Bitmap and Camera when composable is disposed to prevent memory/resource leak
    DisposableEffect(Unit) {
        onDispose {
            capturedBitmap?.recycle()
            cameraProvider?.unbindAll()
        }
    }

    // Initialize camera provider when previewView is available
    LaunchedEffect(previewView) {
        val view = previewView ?: return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build().also {
                        it.surfaceProvider = view.surfaceProvider
                    }

                val newImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(resolutionSelector)
                    .build()
                imageCapture = newImageCapture

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newImageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                currentOnCameraInitError()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (capturedBitmap != null) {
                // Photo Preview Mode
                PhotoPreviewContent(
                    bitmap = capturedBitmap!!,
                    onRetake = {
                        capturedBitmap?.recycle()
                        capturedBitmap = null
                    },
                    onKeep = {
                        capturedBitmap?.let { bitmap ->
                            saveBitmapToMediaStore(
                                context = context,
                                bitmap = bitmap,
                                onSuccess = {
                                    bitmap.recycle()
                                    onClose()
                                },
                                onError = onSaveError
                            )
                        }
                    },
                    onClose = {
                        capturedBitmap?.recycle()
                        capturedBitmap = null
                        onClose()
                    }
                )
            } else {
                // Camera Mode
                CameraCaptureContent(
                    onPreviewViewCreate = { previewView = it },
                    isFlashEnabled = isFlashEnabled,
                    onFlashToggle = { isFlashEnabled = !isFlashEnabled },
                    isCapturing = isCapturing,
                    onCapture = {
                        imageCapture?.let { capture ->
                            isCapturing = true
                            capture.flashMode = if (isFlashEnabled) {
                                ImageCapture.FLASH_MODE_ON
                            } else {
                                ImageCapture.FLASH_MODE_OFF
                            }
                            takePictureForPreview(
                                context = context,
                                imageCapture = capture,
                                scope = scope,
                                onCaptured = { bitmap ->
                                    capturedBitmap = bitmap
                                    isCapturing = false
                                },
                                onError = {
                                    isCapturing = false
                                    onCaptureError()
                                }
                            )
                        }
                    },
                    onClose = {
                        capturedBitmap?.recycle()
                        capturedBitmap = null
                        onClose()
                    }
                )
            }
        }
    }
}