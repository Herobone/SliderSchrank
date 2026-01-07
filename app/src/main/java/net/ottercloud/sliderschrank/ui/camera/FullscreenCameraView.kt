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
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ottercloud.sliderschrank.util.BackgroundRemover
import net.ottercloud.sliderschrank.util.saveTransparentBitmapToMediaStore
import net.ottercloud.sliderschrank.util.takePictureForPreview

private const val TAG = "FullscreenCameraView"

/**
 * Represents the current state of the camera view flow.
 */
private enum class CameraViewState {
    /** Camera is active, ready to capture */
    CAMERA,

    /** Showing preview of captured original image */
    ORIGINAL_PREVIEW,

    /** Processing background removal */
    PROCESSING,

    /** Showing preview of image with removed background */
    TRANSPARENT_PREVIEW
}

@Composable
fun FullscreenCameraView(
    onClose: () -> Unit,
    onSaveError: () -> Unit,
    onCameraInitError: () -> Unit,
    onCaptureError: () -> Unit,
    onBackgroundRemovalError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Use rememberUpdatedState to ensure DisposableEffect always has the latest callback reference
    val currentOnCameraInitError by rememberUpdatedState(onCameraInitError)

    var viewState by remember { mutableStateOf(CameraViewState.CAMERA) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var transparentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var processingJob by remember { mutableStateOf<Job?>(null) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // Helper function to clean up all bitmaps and reset state
    fun cleanupAndResetToCamera() {
        capturedBitmap?.recycle()
        capturedBitmap = null
        transparentBitmap?.recycle()
        transparentBitmap = null
        viewState = CameraViewState.CAMERA
    }

    // Helper function to clean up all bitmaps
    fun cleanupAllBitmaps() {
        capturedBitmap?.recycle()
        capturedBitmap = null
        transparentBitmap?.recycle()
        transparentBitmap = null
    }

    // Cleanup Bitmap and Camera when composable is disposed to prevent memory/resource leak
    DisposableEffect(lifecycleOwner) {
        try {
            cameraController.bindToLifecycle(lifecycleOwner)
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            currentOnCameraInitError()
        }
        onDispose {
            processingJob?.cancel()
            cleanupAllBitmaps()
            cameraController.unbind()
            BackgroundRemover.close()
        }
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
            when (viewState) {
                CameraViewState.CAMERA -> {
                    // Camera Mode
                    CameraCaptureContent(
                        onPreviewViewCreate = { view ->
                            view.controller = cameraController
                        },
                        isFlashEnabled = isFlashEnabled,
                        onFlashToggle = { isFlashEnabled = !isFlashEnabled },
                        isCapturing = isCapturing,
                        onCapture = {
                            isCapturing = true
                            cameraController.imageCaptureFlashMode = if (isFlashEnabled) {
                                ImageCapture.FLASH_MODE_ON
                            } else {
                                ImageCapture.FLASH_MODE_OFF
                            }
                            takePictureForPreview(
                                context = context,
                                cameraController = cameraController,
                                scope = scope,
                                onCaptured = { bitmap ->
                                    capturedBitmap = bitmap
                                    isCapturing = false
                                    viewState = CameraViewState.ORIGINAL_PREVIEW
                                },
                                onError = {
                                    isCapturing = false
                                    onCaptureError()
                                }
                            )
                        },
                        onClose = {
                            cleanupAllBitmaps()
                            onClose()
                        }
                    )
                }

                CameraViewState.ORIGINAL_PREVIEW -> {
                    // Original Photo Preview - user decides to keep or retake
                    PhotoPreviewContent(
                        bitmap = capturedBitmap!!,
                        onRetake = {
                            capturedBitmap?.recycle()
                            capturedBitmap = null
                            viewState = CameraViewState.CAMERA
                        },
                        onKeep = {
                            // Start background removal
                            viewState = CameraViewState.PROCESSING
                            processingJob = scope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    BackgroundRemover.removeBackground(capturedBitmap!!)
                                }

                                withContext(Dispatchers.Main) {
                                    if (result != null) {
                                        transparentBitmap = result
                                        // Recycle the original bitmap as we don't need it anymore
                                        capturedBitmap?.recycle()
                                        capturedBitmap = null
                                        viewState = CameraViewState.TRANSPARENT_PREVIEW
                                    } else {
                                        // Background removal failed
                                        Log.e(TAG, "Background removal failed")
                                        cleanupAndResetToCamera()
                                        onBackgroundRemovalError()
                                    }
                                }
                                processingJob = null
                            }
                        },
                        onClose = {
                            cleanupAllBitmaps()
                            onClose()
                        }
                    )
                }

                CameraViewState.PROCESSING -> {
                    // Show processing indicator while removing background
                    PhotoPreviewContent(
                        bitmap = capturedBitmap!!,
                        onRetake = { /* Disabled during processing */ },
                        onKeep = { /* Disabled during processing */ },
                        onClose = { /* Disabled during processing */ },
                        isProcessing = true
                    )
                }

                CameraViewState.TRANSPARENT_PREVIEW -> {
                    // Transparent Photo Preview - user decides to save or discard
                    PhotoPreviewContent(
                        bitmap = transparentBitmap!!,
                        onRetake = {
                            // User doesn't like the result, go back to camera
                            cleanupAndResetToCamera()
                        },
                        onKeep = {
                            // Save only the transparent image
                            transparentBitmap?.let { bitmap ->
                                saveTransparentBitmapToMediaStore(
                                    context = context,
                                    bitmap = bitmap,
                                    onSuccess = {
                                        cleanupAllBitmaps()
                                        onClose()
                                    },
                                    onError = {
                                        // Keep the transparent bitmap so user can retry saving
                                        onSaveError()
                                    }
                                )
                            }
                        },
                        onClose = {
                            // User cancels - cleanup and close without state reset
                            cleanupAllBitmaps()
                            onClose()
                        }
                    )
                }
            }
        }
    }
}