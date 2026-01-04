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
package net.ottercloud.sliderschrank

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import net.ottercloud.sliderschrank.ui.theme.KeepGreen
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

private const val TAG = "CameraScreen"

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.close),
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showPermissionPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var showSaveErrorDialog by remember { mutableStateOf(false) }
    var showFullscreenCamera by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showFullscreenCamera = true
        } else {
            // User denied the permission request.
            // We only get here if we actually launched the permission dialog (checked in button handler).
            // Show simple denied dialog - if user selects "Don't ask again", the next button click
            // will detect this via shouldShowRationale and show the settings dialog instead.
            showPermissionDeniedDialog = true
        }
    }

    // Permission Denied Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text(stringResource(R.string.camera_permission_denied)) },
            text = { Text(stringResource(R.string.camera_permission_denied_message)) },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Permission Permanently Denied Dialog -> redirect to settings
    if (showPermissionPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionPermanentlyDeniedDialog = false },
            title = { Text(stringResource(R.string.camera_permission_denied)) },
            text = { Text(stringResource(R.string.camera_permission_permanently_denied_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionPermanentlyDeniedDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionPermanentlyDeniedDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Save Error Dialog
    if (showSaveErrorDialog) {
        AlertDialog(
            onDismissRequest = { showSaveErrorDialog = false },
            title = { Text(stringResource(R.string.image_save_failed)) },
            text = { Text(stringResource(R.string.image_save_failed_message)) },
            confirmButton = {
                TextButton(onClick = { showSaveErrorDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Fullscreen Camera Dialog
    if (showFullscreenCamera) {
        FullscreenCameraView(
            onClose = { showFullscreenCamera = false },
            onSaveError = { showSaveErrorDialog = true }
        )
    }

    // Main Camera Screen
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Placeholder Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                    "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                    "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
                    "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
                    "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in " +
                    "culpa qui officia deserunt mollit anim id est laborum.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    // Check if permission is already granted
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        // Permission granted - reset the tracking flag so the normal flow works
                        // again if the user later revokes the permission in system settings
                        context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE)
                            .edit { putBoolean("camera_permission_asked", false) }
                        showFullscreenCamera = true
                    } else {
                        // Permission not granted - check if we can still ask
                        val prefs = context.getSharedPreferences(
                            "camera_prefs",
                            Context.MODE_PRIVATE
                        )
                        val hasAskedBefore = prefs.getBoolean(
                            "camera_permission_asked",
                            false
                        )
                        if (!hasAskedBefore) {
                            // First time asking -> set flag and launch permission request
                            prefs.edit {
                                putBoolean("camera_permission_asked", true)
                            }
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            // Already asked before - check if we can ask again
                            val activity = context as? android.app.Activity
                            val shouldShowRationale =
                                activity?.shouldShowRequestPermissionRationale(
                                    Manifest.permission.CAMERA
                                ) ?: false

                            if (shouldShowRationale) {
                                // User denied once, can still ask again
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                // Permanently denied -> show settings dialog
                                showPermissionPermanentlyDeniedDialog = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.take_picture))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    handleImportFromGallery(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.import_from_gallery))
            }
        }
    }
}

@Composable
private fun FullscreenCameraView(onClose: () -> Unit, onSaveError: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

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
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 70.dp),
                    contentScale = ContentScale.Fit
                )

                // Close Button (top right)
                CloseButton(
                    onClick = {
                        capturedBitmap?.recycle()
                        capturedBitmap = null
                        onClose()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )

                // Preview Control Bar at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retake Button (white circle with gray refresh icon)
                    IconButton(
                        onClick = {
                            capturedBitmap?.recycle()
                            capturedBitmap = null
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.retake_photo),
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Keep Button (green circle with white check icon)
                    IconButton(
                        onClick = {
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
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = KeepGreen,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.keep_photo),
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else {
                // Camera Mode
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                        }.also { previewView = it }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 70.dp)
                )

                // Close Button (top right)
                CloseButton(
                    onClick = {
                        capturedBitmap?.recycle()
                        capturedBitmap = null
                        onClose()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )

                // Camera Control Bar at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash Toggle Button
                    IconButton(
                        onClick = {
                            isFlashEnabled = !isFlashEnabled
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isFlashEnabled) {
                                Icons.Default.FlashOn
                            } else {
                                Icons.Default.FlashOff
                            },
                            contentDescription = if (isFlashEnabled) {
                                stringResource(R.string.flash_on)
                            } else {
                                stringResource(R.string.flash_off)
                            },
                            tint = if (isFlashEnabled) Color.Yellow else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Capture Button
                    IconButton(
                        onClick = {
                            imageCapture?.let { capture ->
                                // Set flash mode right before capture
                                capture.flashMode = if (isFlashEnabled) {
                                    ImageCapture.FLASH_MODE_ON
                                } else {
                                    ImageCapture.FLASH_MODE_OFF
                                }
                                takePictureForPreview(
                                    context = context,
                                    imageCapture = capture,
                                    onCaptured = { bitmap ->
                                        capturedBitmap = bitmap
                                    },
                                    onError = onSaveError
                                )
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = stringResource(R.string.take_picture),
                            tint = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                    }

                    // Placeholder for symmetry (same size as flash button)
                    Box(modifier = Modifier.size(56.dp))
                }
            }
        }
    }
}

private fun takePictureForPreview(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Bitmap) -> Unit,
    onError: () -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees)
                image.close()
                onCaptured(rotatedBitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                onError()
            }
        }
    )
}

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
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

private fun saveBitmapToMediaStore(
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
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

@Suppress("UNUSED_PARAMETER")
private fun handleImportFromGallery(context: Context) {
    // TODO: Hier wird später die GalleryService verwendet
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraScreenPreview() {
    SliderSchrankTheme {
        CameraScreen()
    }
}