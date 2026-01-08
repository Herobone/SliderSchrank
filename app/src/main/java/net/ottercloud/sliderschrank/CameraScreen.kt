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
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import net.ottercloud.sliderschrank.ui.camera.BackgroundRemovalErrorDialog
import net.ottercloud.sliderschrank.ui.camera.CameraInitErrorDialog
import net.ottercloud.sliderschrank.ui.camera.CaptureErrorDialog
import net.ottercloud.sliderschrank.ui.camera.FullscreenCameraView
import net.ottercloud.sliderschrank.ui.camera.PermissionDeniedDialog
import net.ottercloud.sliderschrank.ui.camera.PermissionPermanentlyDeniedDialog
import net.ottercloud.sliderschrank.ui.camera.SaveErrorDialog
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(onImageSave: (android.net.Uri) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE)

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showPermissionPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var showSaveErrorDialog by remember { mutableStateOf(false) }
    var showCameraInitErrorDialog by remember { mutableStateOf(false) }
    var showCaptureErrorDialog by remember { mutableStateOf(false) }
    var showBackgroundRemovalErrorDialog by remember { mutableStateOf(false) }
    var showFullscreenCamera by remember { mutableStateOf(false) }
    var permissionRequestLaunched by remember { mutableStateOf(false) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            resetPermissionFlag(context)
        }
    }

    LaunchedEffect(cameraPermissionState.status) {
        val hasAskedBefore = prefs.getBoolean("camera_permission_asked", false)
        permissionPermanentlyDenied = hasAskedBefore &&
            !cameraPermissionState.status.isGranted &&
            !cameraPermissionState.status.shouldShowRationale

        if (!permissionRequestLaunched) return@LaunchedEffect

        when {
            cameraPermissionState.status.isGranted -> {
                permissionRequestLaunched = false
            }
            cameraPermissionState.status.shouldShowRationale -> {
                showPermissionDeniedDialog = true
                permissionRequestLaunched = false
            }
            else -> {
                showPermissionPermanentlyDeniedDialog = true
                permissionRequestLaunched = false
            }
        }
    }

    // Dialogs
    if (showPermissionDeniedDialog) {
        PermissionDeniedDialog(onDismiss = { showPermissionDeniedDialog = false })
    }

    if (showPermissionPermanentlyDeniedDialog) {
        PermissionPermanentlyDeniedDialog(
            context = context,
            onDismiss = { showPermissionPermanentlyDeniedDialog = false }
        )
    }

    if (showSaveErrorDialog) {
        SaveErrorDialog(onDismiss = { showSaveErrorDialog = false })
    }

    if (showCameraInitErrorDialog) {
        CameraInitErrorDialog(onDismiss = { showCameraInitErrorDialog = false })
    }

    if (showCaptureErrorDialog) {
        CaptureErrorDialog(onDismiss = { showCaptureErrorDialog = false })
    }

    if (showBackgroundRemovalErrorDialog) {
        BackgroundRemovalErrorDialog(onDismiss = { showBackgroundRemovalErrorDialog = false })
    }

    // Fullscreen Camera Dialog
    if (showFullscreenCamera) {
        FullscreenCameraView(
            onClose = { showFullscreenCamera = false },
            onImageSave = { uri ->
                showFullscreenCamera = false
                onImageSave(uri)
            },
            onSaveError = { showSaveErrorDialog = true },
            onCameraInitError = {
                showFullscreenCamera = false
                showCameraInitErrorDialog = true
            },
            onCaptureError = { showCaptureErrorDialog = true },
            onBackgroundRemovalError = { showBackgroundRemovalErrorDialog = true }
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
                    if (cameraPermissionState.status.isGranted) {
                        showFullscreenCamera = true
                    } else if (permissionPermanentlyDenied) {
                        showPermissionPermanentlyDeniedDialog = true
                    } else if (cameraPermissionState.status.shouldShowRationale) {
                        permissionRequestLaunched = true
                        cameraPermissionState.launchPermissionRequest()
                    } else {
                        val hasAskedBefore = prefs.getBoolean("camera_permission_asked", false)

                        if (!hasAskedBefore) {
                            prefs.edit { putBoolean("camera_permission_asked", true) }
                            permissionRequestLaunched = true
                            cameraPermissionState.launchPermissionRequest()
                        } else {
                            showPermissionPermanentlyDeniedDialog = true
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

private fun handleImportFromGallery(context: Context) {
    // TODO: Hier wird später die GalleryService verwendet
}

private fun resetPermissionFlag(context: Context) {
    context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE).edit {
        putBoolean("camera_permission_asked", false)
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraScreenPreview() {
    SliderSchrankTheme {
        CameraScreen({})
    }
}