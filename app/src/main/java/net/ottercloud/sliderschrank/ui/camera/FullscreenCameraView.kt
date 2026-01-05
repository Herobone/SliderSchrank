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

    // Use rememberUpdatedState to ensure DisposableEffect always has the latest callback reference
    val currentOnCameraInitError by rememberUpdatedState(onCameraInitError)

    var isFlashEnabled by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
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
            capturedBitmap?.recycle()
            cameraController.unbind()
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
                                onError = {
                                    bitmap.recycle()
                                    capturedBitmap = null
                                    onSaveError()
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
            } else {
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
                            },
                            onError = {
                                isCapturing = false
                                onCaptureError()
                            }
                        )
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